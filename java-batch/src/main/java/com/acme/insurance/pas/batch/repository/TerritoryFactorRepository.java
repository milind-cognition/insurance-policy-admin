package com.acme.insurance.pas.batch.repository;

import com.acme.insurance.pas.batch.model.TerritoryFactor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Loads territory rating factors from the TERRITORY_FACTORS table.
 * Implements what COBOL paragraph 2000-LOAD-RATING-TABLES (lines 102-115)
 * declares but leaves stubbed.
 */
@Repository
public class TerritoryFactorRepository {

    private final JdbcTemplate jdbcTemplate;

    public TerritoryFactorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TerritoryFactor> findByCodeAndDate(String territoryCode, LocalDate asOfDate) {
        if (territoryCode == null || territoryCode.isBlank()) {
            return Optional.empty();
        }
        String sql =
                "SELECT TERRITORY_CODE, RATING_FACTOR, EFFECTIVE_DATE " +
                "FROM TERRITORY_FACTORS " +
                "WHERE TERRITORY_CODE = ? AND EFFECTIVE_DATE <= ? " +
                "ORDER BY EFFECTIVE_DATE DESC " +
                "FETCH FIRST 1 ROWS ONLY";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TerritoryFactor tf = new TerritoryFactor();
            tf.setTerritoryCode(rs.getString("TERRITORY_CODE"));
            tf.setRatingFactor(rs.getBigDecimal("RATING_FACTOR"));
            tf.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
            return tf;
        }, territoryCode, java.sql.Date.valueOf(asOfDate)).stream().findFirst();
    }
}
