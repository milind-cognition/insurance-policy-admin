package com.acme.dropin.contract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * IBM packed decimal (COBOL {@code COMP-3}) encoding.
 *
 * <p>Two digits per byte, low-order nibble of the last byte carries the sign
 * (0x0C positive, 0x0D negative, 0x0F unsigned). A {@code PIC S9(9)V99} field
 * therefore occupies 6 bytes and holds 11 digits with an implied 2-digit scale.
 */
public final class PackedDecimal {

    private PackedDecimal() {
    }

    public static byte[] encode(BigDecimal value, PicClause pic) {
        int digits = pic.totalDigits();
        BigDecimal scaled = value.setScale(pic.decimalDigits(), RoundingMode.DOWN);
        BigInteger unscaled = scaled.unscaledValue();
        boolean negative = unscaled.signum() < 0;
        String plain = unscaled.abs().toString();
        if (plain.length() > digits) {
            // COBOL truncates high-order digits on overflow rather than failing.
            plain = plain.substring(plain.length() - digits);
        }
        String padded = "0".repeat(digits - plain.length()) + plain;

        byte[] out = new byte[pic.byteLength()];
        // Digits are right aligned; an odd digit count leaves a leading zero nibble.
        String nibbles = (digits % 2 == 0 ? "0" : "") + padded;
        for (int i = 0; i < out.length - 1; i++) {
            int high = nibbles.charAt(i * 2) - '0';
            int low = nibbles.charAt(i * 2 + 1) - '0';
            out[i] = (byte) ((high << 4) | low);
        }
        int lastDigit = nibbles.charAt(nibbles.length() - 1) - '0';
        int sign = !pic.signed() ? 0x0F : negative ? 0x0D : 0x0C;
        out[out.length - 1] = (byte) ((lastDigit << 4) | sign);
        return out;
    }

    public static BigDecimal decode(byte[] image, int offset, PicClause pic) {
        int length = pic.byteLength();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < length - 1; i++) {
            int b = image[offset + i] & 0xFF;
            digits.append((char) ('0' + (b >> 4)));
            digits.append((char) ('0' + (b & 0x0F)));
        }
        int last = image[offset + length - 1] & 0xFF;
        digits.append((char) ('0' + (last >> 4)));
        int sign = last & 0x0F;
        BigDecimal value = new BigDecimal(new BigInteger(digits.toString()), pic.decimalDigits());
        return sign == 0x0D ? value.negate() : value;
    }

    public static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
