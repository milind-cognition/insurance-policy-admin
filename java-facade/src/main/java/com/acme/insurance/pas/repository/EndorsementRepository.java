package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.model.Endorsement;
import com.acme.insurance.pas.model.EndorsementId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EndorsementRepository extends JpaRepository<Endorsement, EndorsementId> {

    @Query("SELECT COALESCE(MAX(e.endorsementSeq), 0) FROM Endorsement e " +
           "WHERE e.policyNumber = :policyNumber")
    int findMaxEndorsementSeqByPolicyNumber(@Param("policyNumber") String policyNumber);
}
