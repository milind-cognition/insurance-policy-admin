package com.acme.dropin.contract;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decimal semantics of the contract.
 *
 * <p>Amounts are two-decimal fixed point everywhere: {@code DECIMAL(n,2)} in
 * DB2, {@code COMP-3} with {@code V99} in the copybooks, {@code 1250.00} in the
 * JSON body. Scale is part of the contract, so it is enforced in one place
 * rather than trusted to whatever a query happened to return.
 */
public final class Money {

    public static final int SCALE = 2;

    private Money() {
    }

    /** Pins a value to contract scale, rejecting a change in value. */
    public static BigDecimal pin(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * COBOL {@code COMPUTE} into a {@code V99} field truncates the excess digits;
     * it does not round half up. {@code 1250.00 * 1.05 = 1312.50} is exact, but
     * {@code 1249.99 * 1.05 = 1312.4895} stores as {@code 1312.48}, not
     * {@code 1312.49}. This is the single most common silent difference when
     * COBOL arithmetic is re-implemented in a language whose default is rounding.
     */
    public static BigDecimal truncateToScale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.DOWN);
    }

    public static BigDecimal roundHalfUpToScale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
