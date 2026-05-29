-- select_territory_factors.sql
-- Ported from PREMBAT.cbl lines 104-109 (TERR_CURSOR)
-- Loads the most recent territory rating factor per territory code
-- effective as of today.
SELECT t.TERRITORY_CODE, t.RATING_FACTOR
FROM TERRITORY_FACTORS t
INNER JOIN (
    SELECT TERRITORY_CODE, MAX(EFFECTIVE_DATE) AS MAX_EFF
    FROM TERRITORY_FACTORS
    WHERE EFFECTIVE_DATE <= CURRENT_DATE
    GROUP BY TERRITORY_CODE
) latest ON t.TERRITORY_CODE = latest.TERRITORY_CODE
       AND t.EFFECTIVE_DATE = latest.MAX_EFF
ORDER BY t.TERRITORY_CODE
