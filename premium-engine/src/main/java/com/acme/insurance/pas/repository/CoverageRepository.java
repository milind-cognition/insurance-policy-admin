package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.entity.Coverage;
import com.acme.insurance.pas.entity.CoverageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoverageRepository extends JpaRepository<Coverage, CoverageId> {

    List<Coverage> findByPolicyNumberAndStatus(String policyNumber, String status);
}
