"""Tests for the parallelized premium batch pipeline."""

from decimal import Decimal

import pytest

from pipelines.premium_batch import (
    BASE_RATES,
    DEFAULT_BASE_RATE,
    REGULATORY_SURCHARGE,
    TAX_RATE,
    RatingFactors,
    calculate_premium,
)


@pytest.fixture
def empty_factors():
    return RatingFactors()


@pytest.fixture
def loaded_factors():
    return RatingFactors(
        territory={"TERR01": Decimal("1.1500"), "TERR02": Decimal("0.9000")},
        class_code={"CLS01": Decimal("1.2500"), "CLS02": Decimal("0.8000")},
    )


class TestCalculatePremium:
    """Verify premium calculation matches legacy PREMBAT logic."""

    @pytest.mark.parametrize(
        "policy_type,expected_base",
        [
            ("AUT", Decimal("850.00")),
            ("HOM", Decimal("1200.00")),
            ("COM", Decimal("5000.00")),
            ("LIF", Decimal("400.00")),
            ("HLT", Decimal("3500.00")),
            ("UNK", DEFAULT_BASE_RATE),
        ],
    )
    def test_base_rate_by_policy_type(self, empty_factors, policy_type, expected_base):
        result = calculate_premium(
            policy_type, Decimal("0"), Decimal("0"), Decimal("0"),
            None, None, empty_factors,
        )
        assert result["base_rate"] == expected_base

    def test_default_factors_are_one(self, empty_factors):
        result = calculate_premium(
            "AUT", Decimal("0"), Decimal("0"), Decimal("0"),
            None, None, empty_factors,
        )
        assert result["territory_factor"] == Decimal("1.0000")
        assert result["class_factor"] == Decimal("1.0000")
        assert result["experience_mod"] == Decimal("1.0000")
        assert result["schedule_mod"] == Decimal("1.0000")

    def test_surcharge_is_flat_25(self, empty_factors):
        result = calculate_premium(
            "AUT", Decimal("0"), Decimal("0"), Decimal("0"),
            None, None, empty_factors,
        )
        assert result["surcharge_amt"] == Decimal("25.00")

    def test_total_premium_formula_no_factors(self, empty_factors):
        """total = base + (base * tax_rate) + surcharge when all factors = 1."""
        for ptype, base in BASE_RATES.items():
            result = calculate_premium(
                ptype, Decimal("0"), Decimal("0"), Decimal("0"),
                None, None, empty_factors,
            )
            expected_tax = (base * TAX_RATE).quantize(Decimal("0.01"))
            expected_total = base + expected_tax + REGULATORY_SURCHARGE
            assert result["total_premium"] == expected_total, (
                f"Failed for {ptype}: {result['total_premium']} != {expected_total}"
            )

    def test_territory_factor_applied(self, loaded_factors):
        result = calculate_premium(
            "AUT", Decimal("0"), Decimal("0"), Decimal("0"),
            "TERR01", None, loaded_factors,
        )
        assert result["territory_factor"] == Decimal("1.1500")
        expected_terr = (Decimal("850.00") * Decimal("1.1500")).quantize(Decimal("0.01"))
        expected_tax = (expected_terr * TAX_RATE).quantize(Decimal("0.01"))
        expected_total = expected_terr + expected_tax + REGULATORY_SURCHARGE
        assert result["total_premium"] == expected_total

    def test_class_factor_applied(self, loaded_factors):
        result = calculate_premium(
            "HOM", Decimal("0"), Decimal("0"), Decimal("0"),
            None, "CLS01", loaded_factors,
        )
        assert result["class_factor"] == Decimal("1.2500")

    def test_both_factors_applied(self, loaded_factors):
        result = calculate_premium(
            "COM", Decimal("0"), Decimal("0"), Decimal("0"),
            "TERR02", "CLS02", loaded_factors,
        )
        base = Decimal("5000.00")
        terr = (base * Decimal("0.9000")).quantize(Decimal("0.01"))
        cls = (terr * Decimal("0.8000")).quantize(Decimal("0.01"))
        mod = cls  # experience_mod = 1.0
        tax = (mod * TAX_RATE).quantize(Decimal("0.01"))
        expected = mod + tax + REGULATORY_SURCHARGE
        assert result["total_premium"] == expected

    def test_unknown_territory_uses_default(self, loaded_factors):
        result = calculate_premium(
            "AUT", Decimal("0"), Decimal("0"), Decimal("0"),
            "NOPE", None, loaded_factors,
        )
        assert result["territory_factor"] == Decimal("1.0000")

    def test_discount_pct_is_zero(self, empty_factors):
        result = calculate_premium(
            "AUT", Decimal("0"), Decimal("0"), Decimal("0"),
            None, None, empty_factors,
        )
        assert result["discount_pct"] == Decimal("0.00")

    def test_auto_premium_exact_values(self, empty_factors):
        """Verify exact values for AUT with no factors — matches legacy COBOL output."""
        result = calculate_premium(
            "AUT", Decimal("0"), Decimal("0"), Decimal("0"),
            None, None, empty_factors,
        )
        assert result["base_rate"] == Decimal("850.00")
        assert result["tax_amt"] == Decimal("29.75")  # 850 * 0.035
        assert result["surcharge_amt"] == Decimal("25.00")
        assert result["total_premium"] == Decimal("904.75")  # 850 + 29.75 + 25


class TestPartitionResult:
    """Test PartitionResult dataclass defaults."""

    def test_defaults(self):
        from pipelines.premium_batch import PartitionResult

        r = PartitionResult(policy_type="AUT")
        assert r.policies_read == 0
        assert r.policies_updated == 0
        assert r.policies_error == 0
        assert r.errors == []
