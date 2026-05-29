package com.acme.insurance.pas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TERRITORY_FACTORS", schema = "ACMEINS")
@IdClass(TerritoryFactorId.class)
public class TerritoryFactor {

    @Id
    @Column(name = "TERRITORY_CODE", length = 6, nullable = false)
    private String territoryCode;

    @Id
    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "RATING_FACTOR", precision = 7, scale = 4, nullable = false)
    private BigDecimal ratingFactor;

    public TerritoryFactor() {
    }

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
