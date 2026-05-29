package com.acme.insurance.pas.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class TerritoryFactorId implements Serializable {

    private String territoryCode;
    private LocalDate effectiveDate;

    public TerritoryFactorId() {
    }

    public TerritoryFactorId(String territoryCode, LocalDate effectiveDate) {
        this.territoryCode = territoryCode;
        this.effectiveDate = effectiveDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerritoryFactorId that = (TerritoryFactorId) o;
        return Objects.equals(territoryCode, that.territoryCode)
                && Objects.equals(effectiveDate, that.effectiveDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(territoryCode, effectiveDate);
    }
}
