package com.acme.insurance.pas.batch.repository;

import com.acme.insurance.pas.batch.model.TerritoryFactor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Loads territory rating factors from the TERRITORY_FACTORS table.
 * Implements the lookup that COBOL paragraph 2000-LOAD-RATING-TABLES
 * (PREMBAT.cbl lines 102-115) had stubbed out.
 */
@Repository
public class TerritoryFactorRepository {

    private static final String SELECT_BY_CODE_AND_DATE =
            "SELECT TERRITORY_CODE, EFFECTIVE_DATE, RATING_FACTOR "
                    + "FROM TERRITORY_FACTORS "
                    + "WHERE TERRITORY_CODE = ? AND EFFECTIVE_DATE <= ? "
                    + "ORDER BY EFFECTIVE_DATE DESC";

    private static final String SELECT_ALL_EFFECTIVE =
            "SELECT TERRITORY_CODE, EFFECTIVE_DATE, RATING_FACTOR "
                    + "FROM TERRITORY_FACTORS "
                    + "WHERE EFFECTIVE_DATE <= ? "
                    + "ORDER BY TERRITORY_CODE";

    private final JdbcTemplate jdbcTemplate;

    public TerritoryFactorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TerritoryFactor> findByCodeAndDate(String territoryCode, LocalDate asOfDate) {
        List<TerritoryFactor> results = jdbcTemplate.query(
                SELECT_BY_CODE_AND_DATE,
                (rs, rowNum) -> {
                    TerritoryFactor tf = new TerritoryFactor();
                    tf.setTerritoryCode(rs.getString("TERRITORY_CODE").trim());
                    tf.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
                    tf.setRatingFactor(rs.getBigDecimal("RATING_FACTOR"));
                    return tf;
                },
                territoryCode, asOfDate);
        return results.stream().findFirst();
    }

    public List<TerritoryFactor> findAllEffective(LocalDate asOfDate) {
        return jdbcTemplate.query(
                SELECT_ALL_EFFECTIVE,
                (rs, rowNum) -> {
                    TerritoryFactor tf = new TerritoryFactor();
                    tf.setTerritoryCode(rs.getString("TERRITORY_CODE").trim());
                    tf.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
                    tf.setRatingFactor(rs.getBigDecimal("RATING_FACTOR"));
                    return tf;
                },
                asOfDate);
    }
}
