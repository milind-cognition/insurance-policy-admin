package com.acme.dropin.contract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The policy population both backends are loaded with.
 *
 * <p>Deterministic on purpose: the same 400 policies, the same premiums, the
 * same terms, on every machine and every run, so a parity failure is a fact
 * about the implementations rather than about the seed. Two properties are
 * chosen rather than random:
 *
 * <ul>
 *   <li>Roughly a third of the premiums end in a cent value where
 *       {@code premium * 1.05} has more than two decimal places, which is where
 *       COBOL's truncation and a naive rewrite's rounding disagree.</li>
 *   <li>Some terms expire on 29 February, which is where {@code ADD 10000 TO
 *       POLICY-EXPIRY-DATE} produces a day that does not exist.</li>
 * </ul>
 */
public final class DemoDataset {

    public static final int DEFAULT_SIZE = 400;

    /** 2020-02-29T00:00:00Z - fixed so lastUpdated never depends on when the demo runs. */
    public static final long FIXED_LAST_UPDATED = 1_582_934_400_000L;

    private static final String[] TYPES = {"AUT", "HOM", "COM", "LIF", "HLT"};
    private static final String[] BRANCHES = {"CHI1", "NYC2", "DAL3", "SEA4", "MIA5"};
    private static final String[] COVERAGE_TYPES = {"DWEL", "PERS", "LIAB", "COLL", "COMP"};
    private static final String[] COVERAGE_TEXT = {
            "Dwelling Coverage", "Personal Property", "Liability Coverage",
            "Collision Coverage", "Comprehensive Coverage"};
    private static final String[] TERRITORIES = {"IL0627", "NY1042", "TX0318", "WA0905", "FL2201"};

    private DemoDataset() {
    }

    public static List<PolicyView> policies() {
        return policies(DEFAULT_SIZE);
    }

    public static List<PolicyView> policies(int count) {
        Random random = new Random(20260101L);
        List<PolicyView> policies = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            policies.add(policy(i, random));
        }
        return List.copyOf(policies);
    }

    /** Coverages keyed by policy number, in sequence order, as POLQRY's cursor returns them. */
    public static Map<String, List<CoverageView>> coverages(List<PolicyView> policies) {
        Random random = new Random(20260102L);
        Map<String, List<CoverageView>> byPolicy = new LinkedHashMap<>();
        for (PolicyView policy : policies) {
            int lines = 1 + random.nextInt(3);
            List<CoverageView> coverages = new ArrayList<>(lines);
            BigDecimal remaining = policy.totalPremium();
            for (int seq = 1; seq <= lines; seq++) {
                int type = random.nextInt(COVERAGE_TYPES.length);
                BigDecimal premium = seq == lines
                        ? remaining
                        : Money.truncateToScale(policy.totalPremium()
                                .multiply(BigDecimal.valueOf(30L + random.nextInt(20), 2)));
                remaining = remaining.subtract(premium);
                coverages.add(new CoverageView(
                        policy.policyNumber(),
                        seq,
                        COVERAGE_TYPES[type],
                        COVERAGE_TEXT[type],
                        policy.coverageLimit(),
                        policy.deductible(),
                        premium,
                        policy.effectiveDate(),
                        policy.expiryDate(),
                        "AC",
                        seq == 1 ? 80 : 100,
                        TERRITORIES[type],
                        String.format("R%04d", 1000 + type)));
            }
            byPolicy.put(policy.policyNumber(), List.copyOf(coverages));
        }
        return Map.copyOf(byPolicy);
    }

    private static PolicyView policy(int index, Random random) {
        String number = String.format("PAS-%08d", index);
        String type = TYPES[index % TYPES.length];

        // Every ninth policy runs a term that ends on a leap day.
        boolean leapTerm = index % 9 == 3;
        PasDate effective = leapTerm ? PasDate.of(2023, 2, 28) : PasDate.of(2025, 1 + (index % 12), 1);
        PasDate expiry = leapTerm ? PasDate.of(2024, 2, 29) : effective.plusYearsByArithmetic(1);
        PasDate inception = PasDate.of(2015 + (index % 8), 1 + (index % 12), 1);

        // Premiums land on cent values that do and do not survive * 1.05 cleanly.
        BigDecimal premium = BigDecimal.valueOf(50_000L + index * 137L, 2);
        BigDecimal deductible = BigDecimal.valueOf(50_000L + (index % 5) * 25_000L, 2);
        BigDecimal limit = BigDecimal.valueOf(10_000_000L + (index % 7) * 5_000_000L, 2);

        String status = switch (index % 10) {
            case 7 -> "EX";   // expired: still renewable
            case 8 -> "CN";   // cancelled: POLRNW refuses
            case 9 -> "LP";   // lapsed: POLRNW refuses
            default -> "AC";
        };

        return new PolicyView(
                number,
                type,
                status,
                effective,
                expiry,
                String.format("C%09d", index),
                String.format("AG%04d", 1000 + (index % 40)),
                BRANCHES[index % BRANCHES.length],
                premium,
                deductible,
                limit,
                inception,
                index % 6,
                index % 4 == 0 ? "AP" : "PN",
                20 + random.nextInt(60),
                index % 2 == 0 ? "Y" : "N",
                "Y",
                FIXED_LAST_UPDATED,
                "PASLOAD");
    }
}
