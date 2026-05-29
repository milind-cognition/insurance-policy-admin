package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    List<Policy> findByPolicyStatus(String status);

    List<Policy> findByPolicyholderId(String id);

    @Query("SELECT COALESCE(SUM(p.coverageLimit), 0) FROM Policy p " +
           "WHERE p.branchCode = :branchCode AND p.policyStatus = :status")
    BigDecimal sumCoverageLimitByBranchCodeAndPolicyStatus(
            @Param("branchCode") String branchCode,
            @Param("status") String status);

    @Query(value = "SELECT NEXT VALUE FOR ACMEINS.POLICY_SEQ", nativeQuery = true)
    Long getNextPolicySequence();
}
