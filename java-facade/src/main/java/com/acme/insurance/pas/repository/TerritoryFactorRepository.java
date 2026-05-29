package com.acme.insurance.pas.repository;

import com.acme.insurance.pas.model.TerritoryFactor;
import com.acme.insurance.pas.model.TerritoryFactorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TerritoryFactorRepository
        extends JpaRepository<TerritoryFactor, TerritoryFactorId> {

    List<TerritoryFactor> findByEffectiveDateLessThanEqualOrderByTerritoryCodeAscEffectiveDateAsc(LocalDate date);
}
