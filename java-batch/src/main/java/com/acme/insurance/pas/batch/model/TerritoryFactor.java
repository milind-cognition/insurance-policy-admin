package com.acme.insurance.pas.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maps a row from the DB2 TERRITORY_FACTORS table.
 * Used by PremiumCalculationService to apply territory-based rating adjustments.
 */
public class TerritoryFactor {

    private String territoryCode;
    private BigDecimal ratingFactor;
    private LocalDate effectiveDate;

    public TerritoryFactor() {
    }

    public String getTerritoryCode() {
        return territoryCode;
    }

    public void setTerritoryCode(String territoryCode) {
        this.territoryCode = territoryCode;
    }

    public BigDecimal getRatingFactor() {
        return ratingFactor;
    }

    public void setRatingFactor(BigDecimal ratingFactor) {
        this.ratingFactor = ratingFactor;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
