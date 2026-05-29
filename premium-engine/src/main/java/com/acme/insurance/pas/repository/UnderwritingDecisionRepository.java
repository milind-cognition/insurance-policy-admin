package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.entity.UnderwritingDecision;
import com.acme.insurance.pas.entity.UnderwritingDecisionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnderwritingDecisionRepository
        extends JpaRepository<UnderwritingDecision, UnderwritingDecisionId> {
}
