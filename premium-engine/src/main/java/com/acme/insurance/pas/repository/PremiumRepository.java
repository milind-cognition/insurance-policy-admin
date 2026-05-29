package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.entity.Premium;
import com.acme.insurance.pas.entity.PremiumId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PremiumRepository extends JpaRepository<Premium, PremiumId> {

    List<Premium> findByPolicyNumber(String policyNumber);
}
