package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.model.Premium;
import com.acme.insurance.pas.model.PremiumId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PremiumRepository extends JpaRepository<Premium, PremiumId> {
}
