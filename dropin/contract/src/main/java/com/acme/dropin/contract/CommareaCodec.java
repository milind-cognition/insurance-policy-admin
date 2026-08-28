package com.acme.dropin.contract;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Reads and writes the fixed-width record images that sit underneath the
 * service boundary: the COMMAREA a CICS program is handed.
 *
 * <p>The layout is not hard-coded here. It comes from {@link Copybook}, which
 * parses the copybook the COBOL programs compile against, so an edit to
 * {@code cobol/copybooks/POLICY-RECORD.cpy} moves the byte offsets in this
 * codec too.
 *
 * <p>Encoding is EBCDIC on z/OS and ASCII here; that difference is a transport
 * concern and is called out in CONTRACT.md rather than papered over.
 */
public final class CommareaCodec {

    private final Copybook copybook;

    public CommareaCodec(Copybook copybook) {
        this.copybook = copybook;
    }

    public Copybook copybook() {
        return copybook;
    }

    public byte[] newRecord() {
        byte[] image = new byte[copybook.recordLength()];
        Arrays.fill(image, (byte) ' ');
        for (CopybookField f : copybook.fields()) {
            if (f.pic().kind() == PicClause.Kind.NUMERIC) {
                if (f.pic().usage() == PicClause.Usage.COMP_3) {
                    putDecimal(image, f.name(), BigDecimal.ZERO);
                } else {
                    Arrays.fill(image, f.offset(), f.end(), (byte) '0');
                }
            }
        }
        return image;
    }

    /** {@code PIC X(n)}: left justified, space padded, truncated on overflow. */
    public void putText(byte[] image, String field, String value) {
        CopybookField f = copybook.field(field);
        require(f, PicClause.Kind.ALPHANUMERIC);
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.US_ASCII);
        Arrays.fill(image, f.offset(), f.end(), (byte) ' ');
        System.arraycopy(bytes, 0, image, f.offset(), Math.min(bytes.length, f.length()));
    }

    public String getText(byte[] image, String field) {
        CopybookField f = copybook.field(field);
        require(f, PicClause.Kind.ALPHANUMERIC);
        return new String(image, f.offset(), f.length(), StandardCharsets.US_ASCII);
    }

    /** As {@link #getText} but with the {@code PIC X(n)} padding removed, which is what the facade does. */
    public String getTrimmedText(byte[] image, String field) {
        return getText(image, field).trim();
    }

    /** {@code PIC 9(n)} DISPLAY: right justified, zero padded. */
    public void putInteger(byte[] image, String field, long value) {
        CopybookField f = copybook.field(field);
        require(f, PicClause.Kind.NUMERIC);
        if (f.pic().usage() == PicClause.Usage.COMP_3) {
            putDecimal(image, field, BigDecimal.valueOf(value));
            return;
        }
        String digits = Long.toString(Math.abs(value));
        if (digits.length() > f.length()) {
            digits = digits.substring(digits.length() - f.length());
        }
        String padded = "0".repeat(f.length() - digits.length()) + digits;
        System.arraycopy(padded.getBytes(StandardCharsets.US_ASCII), 0, image, f.offset(), f.length());
    }

    public long getInteger(byte[] image, String field) {
        CopybookField f = copybook.field(field);
        require(f, PicClause.Kind.NUMERIC);
        if (f.pic().usage() == PicClause.Usage.COMP_3) {
            return PackedDecimal.decode(image, f.offset(), f.pic()).longValueExact();
        }
        return Long.parseLong(new String(image, f.offset(), f.length(), StandardCharsets.US_ASCII));
    }

    /** {@code PIC 9(08)} date field holding YYYYMMDD, valid calendar date or not. */
    public void putDate(byte[] image, String field, PasDate date) {
        putInteger(image, field, date == null ? 0 : date.yyyymmdd());
    }

    public PasDate getDate(byte[] image, String field) {
        long value = getInteger(image, field);
        return value == 0 ? null : PasDate.of((int) value);
    }

    /** {@code COMP-3} packed decimal, scale taken from the copybook's V clause. */
    public void putDecimal(byte[] image, String field, BigDecimal value) {
        CopybookField f = copybook.field(field);
        byte[] packed = PackedDecimal.encode(value, f.pic());
        System.arraycopy(packed, 0, image, f.offset(), packed.length);
    }

    public BigDecimal getDecimal(byte[] image, String field) {
        CopybookField f = copybook.field(field);
        return PackedDecimal.decode(image, f.offset(), f.pic());
    }

    private static void require(CopybookField f, PicClause.Kind kind) {
        if (f.pic().kind() != kind) {
            throw new IllegalArgumentException(
                    f.name() + " is " + f.pic() + ", not " + kind);
        }
    }
}
