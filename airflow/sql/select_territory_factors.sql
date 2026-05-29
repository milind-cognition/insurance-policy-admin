-- select_territory_factors.sql
-- Ported from PREMBAT.cbl lines 104-109 (TERR_CURSOR)
-- Loads territory rating factors effective as of today.
SELECT TERRITORY_CODE, RATING_FACTOR
FROM TERRITORY_FACTORS
WHERE EFFECTIVE_DATE <= CURRENT_DATE
ORDER BY TERRITORY_CODE
