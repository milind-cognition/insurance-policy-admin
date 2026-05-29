-- select_active_policies.sql
-- Ported from PREMBAT.cbl lines 118-127 (POL_CURSOR)
-- Selects all active policies for premium recalculation.
SELECT POLICY_NUMBER, POLICY_TYPE, TOTAL_PREMIUM,
       DEDUCTIBLE, COVERAGE_LIMIT,
       EFFECTIVE_DATE, EXPIRY_DATE
FROM POLICIES
WHERE POLICY_STATUS = 'AC'
ORDER BY POLICY_NUMBER
