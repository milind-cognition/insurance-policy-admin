package com.acme.dropin.legacy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * The two arithmetic rules that make COBOL results differ from a rewrite.
 *
 * <p>1. A {@code COMPUTE} without the {@code ROUNDED} phrase truncates when it
 * stores its result. {@code POLRNW} has no {@code ROUNDED} anywhere in it, so
 * every premium it writes is truncated to the two decimal places of
 * {@code PIC S9(09)V99}, never rounded.
 *
 * <p>2. A numeric field holds exactly as many digits as its picture. Storing a
 * larger value drops the high-order digits rather than failing, which is why
 * {@code PIC 9(03)} renewal counts wrap from 999 to 000.
 */
public final class CobolArithmetic {

    /** Enough precision that the intermediate is never the thing that loses digits. */
    private static final MathContext INTERMEDIATE = new MathContext(31, RoundingMode.HALF_UP);

    private CobolArithmetic() {
    }

    /** {@code COMPUTE target = value} where target is {@code PIC S9(n)V99} and ROUNDED is absent. */
    public static BigDecimal storeTruncated(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.DOWN);
    }

    /** {@code COMPUTE WS-NEW-PREMIUM = WS-OLD-PREMIUM * 1.05} - POLRNW 4000-CALCULATE-NEW-PREMIUM. */
    public static BigDecimal applyRateIncrease(BigDecimal oldPremium) {
        return storeTruncated(oldPremium.multiply(new BigDecimal("1.05")), 2);
    }

    /**
     * {@code COMPUTE WS-RATE-CHANGE-PCT = ((WS-NEW-PREMIUM - WS-OLD-PREMIUM) / WS-OLD-PREMIUM) * 100}
     * stored into {@code PIC S9(03)V99 COMP-3} - POLRNW 4000-CALCULATE-NEW-PREMIUM.
     */
    public static BigDecimal rateChangePercent(BigDecimal oldPremium, BigDecimal newPremium) {
        if (oldPremium.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        BigDecimal ratio = newPremium.subtract(oldPremium).divide(oldPremium, INTERMEDIATE);
        return storeTruncated(ratio.multiply(BigDecimal.valueOf(100)), 2);
    }

    /**
     * {@code COMPUTE WS-NEW-PREMIUM = WS-OLD-PREMIUM * (1 + WS-RATE-INCREASE-CAP / 100)}
     * - POLRNW 5000-APPLY-RATE-CAP, reached only when the increase exceeds the cap.
     */
    public static BigDecimal applyRateCap(BigDecimal oldPremium, BigDecimal capPercent) {
        BigDecimal factor = BigDecimal.ONE.add(capPercent.divide(BigDecimal.valueOf(100), INTERMEDIATE));
        return storeTruncated(oldPremium.multiply(factor), 2);
    }

    /** {@code ADD 1 TO POLICY-RENEWAL-COUNT} where the field is {@code PIC 9(03)}. */
    public static int addToPic9(int value, int addend, int digits) {
        int modulus = (int) Math.pow(10, digits);
        return Math.floorMod(value + addend, modulus);
    }
}
