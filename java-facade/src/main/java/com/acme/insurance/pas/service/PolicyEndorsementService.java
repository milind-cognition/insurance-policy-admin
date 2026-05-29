package com.acme.insurance.pas.service;

import com.acme.insurance.pas.model.Endorsement;
import com.acme.insurance.pas.model.EndorsementRequest;
import com.acme.insurance.pas.model.EndorsementResponse;
import com.acme.insurance.pas.model.Policy;
import com.acme.insurance.pas.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Policy Endorsement Service — migrated from COBOL program POLEND.
 *
 * Replicates the COBOL paragraph flow:
 *   1000-INITIALIZE
 *   2000-RECEIVE-ENDORSEMENT (lookup policy)
 *   3000-VALIDATE-ENDORSEMENT
 *   4000-CALCULATE-PRORATA
 *   5000-APPLY-ENDORSEMENT
 *   6000-WRITE-AUDIT-TRAIL (log.info replaces CICS TS queue)
 *   7000-SEND-CONFIRMATION (return response)
 *
 * @see POLEND.cbl, CICS Transaction PEND
 */
@Service
public class PolicyEndorsementService {

    private static final Logger log = LoggerFactory.getLogger(PolicyEndorsementService.class);

    private static final List<String> VALID_ENDORSEMENT_TYPES =
            Arrays.asList("CAD", "CRM", "LCH", "ACH", "CAN");

    @Autowired
    private PolicyRepository policyRepository;

    @Transactional
    public EndorsementResponse processEndorsement(String policyNumber,
            EndorsementRequest request) {

        // 1000-INITIALIZE
        Date currentDate = new Date();

        // 2000-RECEIVE-ENDORSEMENT (FOR UPDATE replicates COBOL WITH RS USE AND KEEP UPDATE LOCKS)
        Policy policy = policyRepository.findByPolicyNumberForUpdate(policyNumber);
        if (policy == null) {
            throw new IllegalArgumentException("POLICY NOT FOUND");
        }

        // 3000-VALIDATE-ENDORSEMENT
        if (!"AC".equals(policy.getPolicyStatus())) {
            throw new IllegalArgumentException(
                    "ONLY ACTIVE POLICIES CAN BE ENDORSED");
        }
        if (request.getEndorsementType() == null
                || request.getEndorsementType().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "ENDORSEMENT TYPE IS REQUIRED");
        }
        if (!VALID_ENDORSEMENT_TYPES.contains(
                request.getEndorsementType().trim())) {
            throw new IllegalArgumentException(
                    "INVALID ENDORSEMENT TYPE");
        }

        // 4000-CALCULATE-PRORATA
        long daysInTerm = daysBetween(
                policy.getEffectiveDate(), policy.getExpiryDate());
        long daysRemaining = daysBetween(currentDate, policy.getExpiryDate());
        if (daysRemaining < 0) {
            daysRemaining = 0;
        }
        BigDecimal prorataFactor;
        if (daysInTerm == 0) {
            prorataFactor = BigDecimal.ONE;
        } else {
            prorataFactor = BigDecimal.valueOf(daysRemaining)
                    .divide(BigDecimal.valueOf(daysInTerm), 6,
                            RoundingMode.HALF_UP);
        }

        // 5000-APPLY-ENDORSEMENT
        int nextSeq = policyRepository.getNextEndorsementSeq(policyNumber);

        Endorsement endorsement = new Endorsement();
        endorsement.setPolicyNumber(policyNumber);
        endorsement.setEndorsementSeq(nextSeq);
        endorsement.setEndorsementType(request.getEndorsementType().trim());
        endorsement.setEffectiveDate(currentDate);
        endorsement.setDescription(request.getDescription());
        endorsement.setPremiumAdjustment(request.getPremiumAdjustment() != null
                ? request.getPremiumAdjustment() : BigDecimal.ZERO);
        endorsement.setProcessedBy("POLEND");

        policyRepository.insertEndorsement(endorsement);

        BigDecimal newTotalPremium = policy.getTotalPremium()
                .add(endorsement.getPremiumAdjustment());
        policyRepository.updatePolicyPremium(
                policyNumber, newTotalPremium, "POLEND");

        // 6000-WRITE-AUDIT-TRAIL (CICS TS queue replaced with log)
        log.info("POLEND audit: policy={}, seq={}, type={}, adjustment={}, " +
                        "newTotal={}, prorata={}",
                policyNumber, nextSeq, endorsement.getEndorsementType(),
                endorsement.getPremiumAdjustment(), newTotalPremium,
                prorataFactor);

        // 7000-SEND-CONFIRMATION
        EndorsementResponse response = new EndorsementResponse();
        response.setPolicyNumber(policyNumber);
        response.setEndorsementSeq(nextSeq);
        response.setEndorsementType(endorsement.getEndorsementType());
        response.setPremiumAdjustment(endorsement.getPremiumAdjustment());
        response.setProrataFactor(prorataFactor);
        response.setNewTotalPremium(newTotalPremium);
        response.setMessage("ENDORSEMENT PROCESSED SUCCESSFULLY");

        return response;
    }

    private long daysBetween(Date start, Date end) {
        long diffMillis = end.getTime() - start.getTime();
        return TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS);
    }
}
