package com.acme.dropin.legacy;

import com.acme.dropin.contract.CoverageCommarea;
import com.acme.dropin.contract.CoverageView;
import com.acme.dropin.contract.DemoDataset;
import com.acme.dropin.contract.PasBackend;
import com.acme.dropin.contract.PasDate;
import com.acme.dropin.contract.PolicyCommarea;
import com.acme.dropin.contract.PolicyView;
import com.acme.dropin.contract.RenewalResult;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The mainframe path, as far as a caller can tell.
 *
 * <p>This is not a CICS region and does not pretend to be one. It is a
 * statement-by-statement transliteration of {@code cobol/programs/POLQRY.cbl}
 * and {@code cobol/programs/POLRNW.cbl}, holding each policy as the
 * {@code POLICY-RECORD} COMMAREA image the COBOL would be handed - so premiums
 * really do pass through {@code COMP-3} with its two decimal places, and text
 * really is space padded to its {@code PIC X(n)} width.
 *
 * <p>The one thing kept outside the record image is {@code LAST_UPDATED}. The
 * copybook field is {@code PIC 9(08)}, a date, but the DB2 column is a
 * {@code TIMESTAMP} and the facade reads the column, not the COMMAREA. Holding
 * it separately is what DB2 does.
 *
 * <p>Paragraph names from the COBOL are quoted on the methods so a reviewer can
 * put the two side by side.
 */
public final class LegacyPolicyAdministration implements PasBackend {

    /** {@code 01 WS-RATE-INCREASE-CAP PIC S9(03)V99 COMP-3 VALUE 15.00} - POLRNW. */
    private static final BigDecimal RATE_INCREASE_CAP = new BigDecimal("15.00");

    /** {@code PERFORM 4100-FETCH-COVERAGE UNTIL ... WS-COV-COUNT >= 20} - POLQRY. */
    private static final int MAX_COVERAGES = 20;

    private final Map<String, Row> policies = new LinkedHashMap<>();
    private final Map<String, List<byte[]>> coverages = new LinkedHashMap<>();
    private final Clock clock;

    public LegacyPolicyAdministration(Clock clock) {
        this.clock = clock;
    }

    public static LegacyPolicyAdministration loadedWithDemoData(Clock clock) {
        LegacyPolicyAdministration backend = new LegacyPolicyAdministration(clock);
        List<PolicyView> policies = DemoDataset.policies();
        Map<String, List<CoverageView>> coverages = DemoDataset.coverages(policies);
        for (PolicyView policy : policies) {
            backend.load(policy, coverages.get(policy.policyNumber()));
        }
        return backend;
    }

    public void load(PolicyView policy, List<CoverageView> policyCoverages) {
        policies.put(policy.policyNumber(),
                new Row(PolicyCommarea.encode(policy), policy.lastUpdated()));
        List<byte[]> images = new ArrayList<>();
        for (CoverageView coverage : policyCoverages) {
            images.add(CoverageCommarea.encode(coverage));
        }
        coverages.put(policy.policyNumber(), images);
    }

    @Override
    public String name() {
        return "mainframe";
    }

    /** POLQRY 2000-READ-POLICY: {@code SQLCODE = 100} is the empty case. */
    @Override
    public Optional<PolicyView> findPolicy(String policyNumber) {
        Row row = policies.get(policyNumber);
        return row == null ? Optional.empty() : Optional.of(row.toView());
    }

    /** POLQRY 4000-READ-COVERAGES: cursor ordered by SEQUENCE_NUM, at most 20 rows fetched. */
    @Override
    public List<CoverageView> findCoverages(String policyNumber) {
        List<byte[]> images = coverages.getOrDefault(policyNumber, List.of());
        List<CoverageView> result = new ArrayList<>(images.size());
        for (byte[] image : images) {
            result.add(CoverageCommarea.decode(image));
        }
        result.sort(Comparator.comparingInt(CoverageView::sequenceNum));
        return result.size() > MAX_COVERAGES ? List.copyOf(result.subList(0, MAX_COVERAGES)) : List.copyOf(result);
    }

    @Override
    public RenewalResult renew(String policyNumber) {
        // 2000-READ-EXISTING-POLICY
        Row row = policies.get(policyNumber);
        if (row == null) {
            return RenewalResult.failed(RenewalResult.NOT_FOUND);
        }
        PolicyView policy = row.toView();

        // 3000-CHECK-RENEWAL-ELIGIBILITY: IF NOT POL-STAT-ACTIVE AND NOT POL-STAT-EXPIRED.
        // The open-claims count is read and then not used - the COBOL says
        // "TODO: Business wants to block renewal if >3 open claims ... never fully
        // implemented - JH 2005" - so nothing here either.
        if (!"AC".equals(policy.policyStatus()) && !"EX".equals(policy.policyStatus())) {
            return RenewalResult.failed(RenewalResult.NOT_ELIGIBLE);
        }

        // 4000-CALCULATE-NEW-PREMIUM
        BigDecimal oldPremium = policy.totalPremium();
        BigDecimal newPremium = CobolArithmetic.applyRateIncrease(oldPremium);
        BigDecimal rateChangePct = CobolArithmetic.rateChangePercent(oldPremium, newPremium);

        // 5000-APPLY-RATE-CAP
        if (rateChangePct.compareTo(RATE_INCREASE_CAP) > 0) {
            newPremium = CobolArithmetic.applyRateCap(oldPremium, RATE_INCREASE_CAP);
        }

        // 6000-CREATE-RENEWAL-TERM
        int renewalCount = CobolArithmetic.addToPic9(policy.renewalCount(), 1, 3);
        PasDate newEffective = policy.expiryDate();
        // ADD 10000 TO POLICY-EXPIRY-DATE GIVING WS-NEW-EXPIRY-DATE. The comment above
        // it in POLRNW reads "Simple arithmetic - does not handle leap years properly".
        PasDate newExpiry = policy.expiryDate().plusYearsByArithmetic(1);
        PolicyView renewed = policy.renewedTo(newEffective, newExpiry, newPremium, renewalCount, "AC", "PN");

        long now = clock.millis();
        policies.put(policyNumber, new Row(PolicyCommarea.encode(withUpdatedBy(renewed, "POLRNW")), now));

        // 7000-UPDATE-COVERAGES: only the AC lines move to the new term.
        List<byte[]> images = coverages.getOrDefault(policyNumber, List.of());
        List<byte[]> updated = new ArrayList<>(images.size());
        for (byte[] image : images) {
            CoverageView coverage = CoverageCommarea.decode(image);
            updated.add("AC".equals(coverage.status())
                    ? CoverageCommarea.encode(coverage.withTerm(newEffective, newExpiry))
                    : image);
        }
        coverages.put(policyNumber, updated);

        return RenewalResult.renewed(policies.get(policyNumber).toView());
    }

    private static PolicyView withUpdatedBy(PolicyView policy, String updatedBy) {
        return new PolicyView(policy.policyNumber(), policy.policyType(), policy.policyStatus(),
                policy.effectiveDate(), policy.expiryDate(), policy.policyholderId(), policy.agentCode(),
                policy.branchCode(), policy.totalPremium(), policy.deductible(), policy.coverageLimit(),
                policy.inceptionDate(), policy.renewalCount(), policy.uwStatus(), policy.riskScore(),
                policy.webIndicator(), policy.apiFlag(), policy.lastUpdated(), updatedBy);
    }

    /** A DB2 row: the record image the COBOL sees, plus the TIMESTAMP column it cannot hold. */
    private record Row(byte[] image, long lastUpdated) {

        PolicyView toView() {
            PolicyView decoded = PolicyCommarea.decode(image);
            return new PolicyView(decoded.policyNumber(), decoded.policyType(), decoded.policyStatus(),
                    decoded.effectiveDate(), decoded.expiryDate(), decoded.policyholderId(),
                    decoded.agentCode(), decoded.branchCode(), decoded.totalPremium(), decoded.deductible(),
                    decoded.coverageLimit(), decoded.inceptionDate(), decoded.renewalCount(),
                    decoded.uwStatus(), decoded.riskScore(), decoded.webIndicator(), decoded.apiFlag(),
                    lastUpdated, decoded.updatedBy());
        }
    }
}
