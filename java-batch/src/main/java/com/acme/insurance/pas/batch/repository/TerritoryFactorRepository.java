package com.acme.insurance.pas.batch.repository;

import com.acme.insurance.pas.batch.model.TerritoryFactor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TerritoryFactorRepository {

    private final JdbcTemplate jdbcTemplate;

    public TerritoryFactorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TerritoryFactor> findByCodeAndDate(String territoryCode, LocalDate asOfDate) {
        String sql = "SELECT TERRITORY_CODE, EFFECTIVE_DATE, RATING_FACTOR "
                + "FROM TERRITORY_FACTORS "
                + "WHERE TERRITORY_CODE = ? AND EFFECTIVE_DATE <= ? "
                + "ORDER BY EFFECTIVE_DATE DESC "
                + "LIMIT 1";

        List<TerritoryFactor> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    TerritoryFactor tf = new TerritoryFactor();
                    tf.setTerritoryCode(rs.getString("TERRITORY_CODE").trim());
                    tf.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
                    tf.setRatingFactor(rs.getBigDecimal("RATING_FACTOR"));
                    return tf;
                },
                territoryCode, asOfDate);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
