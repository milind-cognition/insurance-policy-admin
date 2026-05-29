"""Unit tests for the premium calculation function.

Validates that the Python port of PREMBAT.cbl ``3200-CALCULATE-PREMIUM``
(lines 149-188) produces correct results using ``decimal.Decimal``
throughout, with no floating-point drift.
"""

import unittest
from decimal import Decimal, ROUND_HALF_UP

# Adjust path so the DAG module is importable without Airflow installed
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "dags"))

from premium_batch_dag import (
    BASE_RATES,
    DEFAULT_BASE,
    SURCHARGE,
    TAX_RATE,
    calculate_premium,
)


class TestCalculatePremium(unittest.TestCase):
    """Tests for ``calculate_premium``."""

    # -----------------------------------------------------------------
    # Base rate by policy type
    # -----------------------------------------------------------------

    def test_auto_base_rate(self):
        base, *_ = calculate_premium("AUT", None, {})
        self.assertEqual(base, Decimal("850.00"))

    def test_home_base_rate(self):
        base, *_ = calculate_premium("HOM", None, {})
        self.assertEqual(base, Decimal("1200.00"))

    def test_commercial_base_rate(self):
        base, *_ = calculate_premium("COM", None, {})
        self.assertEqual(base, Decimal("5000.00"))

    def test_life_base_rate(self):
        base, *_ = calculate_premium("LIF", None, {})
        self.assertEqual(base, Decimal("400.00"))

    def test_health_base_rate(self):
        base, *_ = calculate_premium("HLT", None, {})
        self.assertEqual(base, Decimal("3500.00"))

    def test_unknown_type_uses_default(self):
        base, *_ = calculate_premium("XYZ", None, {})
        self.assertEqual(base, DEFAULT_BASE)
        self.assertEqual(base, Decimal("1000.00"))

    # -----------------------------------------------------------------
    # Territory factor
    # -----------------------------------------------------------------

    def test_territory_factor_applied(self):
        factors = {"NY01": Decimal("1.2500")}
        base, terr_f, cls_f, exp_m, tax, surcharge, final = calculate_premium(
            "AUT", "NY01", factors
        )
        self.assertEqual(terr_f, Decimal("1.2500"))
        modified = Decimal("850.00") * Decimal("1.2500")
        self.assertEqual(modified, Decimal("1062.5000"))

    def test_territory_factor_default_when_missing(self):
        _, terr_f, *_ = calculate_premium("AUT", "MISSING", {})
        self.assertEqual(terr_f, Decimal("1.0000"))

    def test_territory_factor_default_when_none(self):
        _, terr_f, *_ = calculate_premium("AUT", None, {})
        self.assertEqual(terr_f, Decimal("1.0000"))

    # -----------------------------------------------------------------
    # Tax calculation
    # -----------------------------------------------------------------

    def test_tax_on_auto(self):
        """Tax = modified_premium * 0.035, rounded HALF_UP to 2 dp."""
        _, _, _, _, tax, _, _ = calculate_premium("AUT", None, {})
        expected = (Decimal("850.00") * Decimal("0.0350")).quantize(
            Decimal("0.01"), rounding=ROUND_HALF_UP
        )
        self.assertEqual(tax, expected)
        self.assertEqual(tax, Decimal("29.75"))

    def test_tax_with_territory_factor(self):
        factors = {"T1": Decimal("1.5000")}
        _, _, _, _, tax, _, _ = calculate_premium("HOM", "T1", factors)
        modified = Decimal("1200.00") * Decimal("1.5000")
        expected = (modified * Decimal("0.0350")).quantize(
            Decimal("0.01"), rounding=ROUND_HALF_UP
        )
        self.assertEqual(tax, expected)
        self.assertEqual(tax, Decimal("63.00"))

    def test_tax_rounding_half_up(self):
        """Ensure ROUND_HALF_UP is used (matching COBOL COMP-3 behaviour)."""
        factors = {"T2": Decimal("1.1111")}
        _, _, _, _, tax, _, _ = calculate_premium("LIF", "T2", factors)
        modified = Decimal("400.00") * Decimal("1.1111")
        expected = (modified * Decimal("0.0350")).quantize(
            Decimal("0.01"), rounding=ROUND_HALF_UP
        )
        self.assertEqual(tax, expected)

    # -----------------------------------------------------------------
    # Surcharge
    # -----------------------------------------------------------------

    def test_surcharge_is_always_25(self):
        for pol_type in ["AUT", "HOM", "COM", "LIF", "HLT", "ZZZ"]:
            _, _, _, _, _, surcharge, _ = calculate_premium(pol_type, None, {})
            self.assertEqual(surcharge, Decimal("25.00"))

    # -----------------------------------------------------------------
    # Final premium = modified + tax + surcharge
    # -----------------------------------------------------------------

    def test_final_premium_auto(self):
        base, terr_f, cls_f, exp_m, tax, surcharge, final = calculate_premium(
            "AUT", None, {}
        )
        modified = base * terr_f * cls_f * exp_m
        self.assertEqual(final, modified + tax + surcharge)

    def test_final_premium_with_factor(self):
        factors = {"NY01": Decimal("1.2500")}
        base, terr_f, cls_f, exp_m, tax, surcharge, final = calculate_premium(
            "COM", "NY01", factors
        )
        modified = base * terr_f * cls_f * exp_m
        self.assertEqual(final, modified + tax + surcharge)

    # -----------------------------------------------------------------
    # Decimal type verification (no floats anywhere)
    # -----------------------------------------------------------------

    def test_all_outputs_are_decimal(self):
        result = calculate_premium("AUT", None, {})
        for value in result:
            self.assertIsInstance(value, Decimal)

    def test_no_floating_point_drift(self):
        """Repeated calculation must be deterministic with Decimal."""
        results = [calculate_premium("COM", None, {}) for _ in range(100)]
        for r in results:
            self.assertEqual(r, results[0])


if __name__ == "__main__":
    unittest.main()
