package com.acme.insurance.pas.batch.repository;

import org.springframework.stereotype.Repository;

@Repository
public class PremiumRepository {

    public static final String INSERT_PREMIUM_SQL =
            "INSERT INTO PREMIUMS "
                    + "(POLICY_NUMBER, COVERAGE_SEQ, "
                    + "TERM_EFFECTIVE_DATE, TERM_EXPIRY_DATE, "
                    + "BASE_RATE, TERRITORY_FACTOR, CLASS_FACTOR, "
                    + "EXPERIENCE_MOD, SCHEDULE_MOD, "
                    + "DISCOUNT_PCT, SURCHARGE_AMT, TAX_AMT, "
                    + "TOTAL_PREMIUM, INSTALLMENT_CODE, "
                    + "CALC_DATE, CALC_BY) "
                    + "VALUES "
                    + "(:policyNumber, :coverageSeq, "
                    + ":termEffDate, :termExpDate, "
                    + ":baseRate, :territoryFactor, :classFactor, "
                    + ":experienceMod, :scheduleMod, "
                    + ":discountPct, :surchargeAmt, :taxAmt, "
                    + ":totalPremium, :installmentCode, "
                    + ":calcDate, :calcBy)";
}
