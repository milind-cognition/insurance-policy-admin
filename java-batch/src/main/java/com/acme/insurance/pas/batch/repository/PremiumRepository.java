package com.acme.insurance.pas.batch.repository;

import com.acme.insurance.pas.batch.model.PremiumRecord;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Writes premium calculation results to the PREMIUMS table.
 * Mirrors the INSERT in COBOL paragraph 3300-WRITE-PREMIUM-RECORD
 * (PREMBAT.cbl lines 202-218).
 *
 * <p>Uses an upsert pattern (UPDATE then INSERT) so the batch job
 * is idempotent — re-running with RunIdIncrementer will not fail
 * with duplicate key violations.
 */
@Repository
public class PremiumRepository {

    static final String UPDATE_PREMIUM =
            "UPDATE PREMIUMS SET "
                    + "TERM_EXPIRY_DATE = :termExpDate, "
                    + "BASE_RATE = :baseRate, "
                    + "TERRITORY_FACTOR = :territoryFactor, "
                    + "CLASS_FACTOR = :classFactor, "
                    + "EXPERIENCE_MOD = :experienceMod, "
                    + "SCHEDULE_MOD = :scheduleMod, "
                    + "DISCOUNT_PCT = :discountPct, "
                    + "SURCHARGE_AMT = :surchargeAmt, "
                    + "TAX_AMT = :taxAmt, "
                    + "TOTAL_PREMIUM = :totalPremium, "
                    + "INSTALLMENT_CODE = :installmentCode, "
                    + "CALC_DATE = :calcDate, "
                    + "CALC_BY = :calcBy "
                    + "WHERE POLICY_NUMBER = :policyNumber "
                    + "AND COVERAGE_SEQ = :coverageSeq "
                    + "AND TERM_EFFECTIVE_DATE = :termEffDate";

    static final String INSERT_PREMIUM =
            "INSERT INTO PREMIUMS "
                    + "(POLICY_NUMBER, COVERAGE_SEQ, TERM_EFFECTIVE_DATE, TERM_EXPIRY_DATE, "
                    + "BASE_RATE, TERRITORY_FACTOR, CLASS_FACTOR, EXPERIENCE_MOD, SCHEDULE_MOD, "
                    + "DISCOUNT_PCT, SURCHARGE_AMT, TAX_AMT, TOTAL_PREMIUM, INSTALLMENT_CODE, "
                    + "CALC_DATE, CALC_BY) "
                    + "VALUES (:policyNumber, :coverageSeq, :termEffDate, :termExpDate, "
                    + ":baseRate, :territoryFactor, :classFactor, :experienceMod, :scheduleMod, "
                    + ":discountPct, :surchargeAmt, :taxAmt, :totalPremium, :installmentCode, "
                    + ":calcDate, :calcBy)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PremiumRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(PremiumRecord record) {
        MapSqlParameterSource params = toParameterSource(record);
        int updated = jdbcTemplate.update(UPDATE_PREMIUM, params);
        if (updated == 0) {
            jdbcTemplate.update(INSERT_PREMIUM, params);
        }
    }

    public static MapSqlParameterSource toParameterSource(PremiumRecord r) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("policyNumber", r.getPolicyNumber());
        params.addValue("coverageSeq", r.getCoverageSeq());
        params.addValue("termEffDate", r.getTermEffDate());
        params.addValue("termExpDate", r.getTermExpDate());
        params.addValue("baseRate", r.getBaseRate());
        params.addValue("territoryFactor", r.getTerritoryFactor());
        params.addValue("classFactor", r.getClassFactor());
        params.addValue("experienceMod", r.getExperienceMod());
        params.addValue("scheduleMod", r.getScheduleMod());
        params.addValue("discountPct", r.getDiscountPct());
        params.addValue("surchargeAmt", r.getSurchargeAmt());
        params.addValue("taxAmt", r.getTaxAmt());
        params.addValue("totalPremium", r.getTotalPremium());
        params.addValue("installmentCode", r.getInstallmentCode());
        params.addValue("calcDate", r.getCalcDate());
        params.addValue("calcBy", r.getCalcBy());
        return params;
    }
}
