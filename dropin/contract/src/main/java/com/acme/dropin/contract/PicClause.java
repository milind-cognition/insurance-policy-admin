package com.acme.dropin.contract;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A COBOL PICTURE clause as used by the PAS copybooks in {@code cobol/copybooks}.
 *
 * <p>Only the forms that actually occur in this repository are supported:
 * {@code PIC X(n)}, {@code PIC 9(n)}, {@code PIC S9(n)V9(m)} and their COMP-3
 * (packed decimal) variants. Anything else is rejected loudly rather than
 * silently mis-parsed, because the whole point of this module is that the
 * layout is not guesswork.
 */
public record PicClause(Kind kind, boolean signed, int integerDigits, int decimalDigits, Usage usage) {

    public enum Kind { ALPHANUMERIC, NUMERIC }

    public enum Usage { DISPLAY, COMP_3, COMP }

    private static final String X_RUN = "(?:X(?:\\(\\d+\\))?)+";
    private static final String NINE_RUN = "(?:9(?:\\(\\d+\\))?)+";
    private static final Pattern ALPHANUMERIC = Pattern.compile("^(" + X_RUN + ")$");
    private static final Pattern NUMERIC = Pattern.compile(
            "^(S)?(" + NINE_RUN + ")(?:V(" + NINE_RUN + "))?$");
    private static final Pattern SYMBOL = Pattern.compile("[X9](?:\\((\\d+)\\))?");

    public static PicClause parse(String pic, Usage usage) {
        String normalized = pic.trim().toUpperCase();
        Matcher alpha = ALPHANUMERIC.matcher(normalized);
        if (alpha.matches()) {
            return new PicClause(Kind.ALPHANUMERIC, false, countSymbols(alpha.group(1)), 0, usage);
        }
        Matcher numeric = NUMERIC.matcher(normalized);
        if (numeric.matches()) {
            boolean signed = numeric.group(1) != null;
            int intDigits = countSymbols(numeric.group(2));
            int decDigits = numeric.group(3) == null ? 0 : countSymbols(numeric.group(3));
            return new PicClause(Kind.NUMERIC, signed, intDigits, decDigits, usage);
        }
        throw new IllegalArgumentException("Unsupported PICTURE clause: " + pic);
    }

    /** {@code 9(09)}, {@code 99} and {@code 9(07)9} all mean a digit count. */
    private static int countSymbols(String run) {
        int total = 0;
        Matcher m = SYMBOL.matcher(run);
        while (m.find()) {
            total += m.group(1) == null ? 1 : Integer.parseInt(m.group(1));
        }
        return total;
    }

    public int totalDigits() {
        return integerDigits + decimalDigits;
    }

    /** Length in bytes of this field inside a record image. */
    public int byteLength() {
        return switch (usage) {
            case DISPLAY -> kind == Kind.ALPHANUMERIC ? integerDigits : totalDigits();
            // COMP-3 stores two digits per byte plus a trailing sign nibble.
            case COMP_3 -> totalDigits() / 2 + 1;
            case COMP -> totalDigits() <= 4 ? 2 : totalDigits() <= 9 ? 4 : 8;
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PIC ");
        if (kind == Kind.ALPHANUMERIC) {
            sb.append(String.format("X(%02d)", integerDigits));
        } else {
            if (signed) {
                sb.append('S');
            }
            sb.append(String.format("9(%02d)", integerDigits));
            if (decimalDigits > 0) {
                sb.append('V').append("9".repeat(decimalDigits));
            }
        }
        if (usage == Usage.COMP_3) {
            sb.append(" COMP-3");
        } else if (usage == Usage.COMP) {
            sb.append(" COMP");
        }
        return sb.toString();
    }
}
