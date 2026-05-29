package com.acme.insurance.pas.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TerritoryFactor {

    private String territoryCode;
    private LocalDate effectiveDate;
    private BigDecimal ratingFactor;

    public String getTerritoryCode() {
        return territoryCode;
    }

    public void setTerritoryCode(String territoryCode) {
        this.territoryCode = territoryCode;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public BigDecimal getRatingFactor() {
        return ratingFactor;
    }

    public void setRatingFactor(BigDecimal ratingFactor) {
        this.ratingFactor = ratingFactor;
    }
}
