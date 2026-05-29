package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.model.Coverage;
import com.acme.insurance.pas.model.CoverageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CoverageRepository extends JpaRepository<Coverage, CoverageId> {

    List<Coverage> findByPolicyNumberOrderBySequenceNum(String policyNumber);

    @Modifying
    @Query("UPDATE Coverage c SET c.effectiveDate = :effectiveDate, c.expiryDate = :expiryDate " +
           "WHERE c.policyNumber = :policyNumber AND c.status = :status")
    int updateDatesByPolicyNumberAndStatus(
            @Param("policyNumber") String policyNumber,
            @Param("status") String status,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("expiryDate") LocalDate expiryDate);
}
