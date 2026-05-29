package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    List<Policy> findByPolicyStatusOrderByPolicyNumber(String policyStatus);

    @Query("SELECT COALESCE(SUM(p.coverageLimit), 0) FROM Policy p " +
           "WHERE p.branchCode = :branchCode AND p.policyStatus = 'AC'")
    BigDecimal sumCoverageLimitByBranchCode(@Param("branchCode") String branchCode);
}
