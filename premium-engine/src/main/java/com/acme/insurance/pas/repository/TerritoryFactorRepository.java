package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.entity.TerritoryFactor;
import com.acme.insurance.pas.entity.TerritoryFactorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TerritoryFactorRepository
        extends JpaRepository<TerritoryFactor, TerritoryFactorId> {

    @Query("SELECT t FROM TerritoryFactor t " +
           "WHERE t.territoryCode = :code AND t.effectiveDate <= :asOf " +
           "ORDER BY t.effectiveDate DESC LIMIT 1")
    Optional<TerritoryFactor> findEffectiveFactor(
            @Param("code") String territoryCode,
            @Param("asOf") LocalDate asOfDate);
}
