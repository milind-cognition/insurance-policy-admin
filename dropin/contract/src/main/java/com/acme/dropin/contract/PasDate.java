package com.acme.dropin.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * A date as the mainframe holds it: the eight digits of {@code PIC 9(08)}.
 *
 * <p>{@link LocalDate} would be the obvious choice and it is the wrong one
 * here. {@code POLRNW} renews a policy with {@code ADD 10000 TO
 * POLICY-EXPIRY-DATE}, which is arithmetic on the number 20240229 and produces
 * 20250229 - a date that does not exist. A type that refuses to hold that value
 * cannot reproduce what the incumbent does, and a demo that cannot reproduce it
 * cannot show it being caught.
 *
 * <p>On the wire this is still {@code "2025-02-29"}, the same
 * {@code YYYY-MM-DD} string the incumbent emits for every valid date.
 */
public record PasDate(int yyyymmdd) implements Comparable<PasDate> {

    public static PasDate of(int yyyymmdd) {
        return new PasDate(yyyymmdd);
    }

    public static PasDate of(int year, int month, int day) {
        return new PasDate(year * 10000 + month * 100 + day);
    }

    public static PasDate of(LocalDate date) {
        return date == null ? null : of(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    @JsonCreator
    public static PasDate parse(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        String digits = iso.replace("-", "");
        if (digits.length() != 8) {
            throw new IllegalArgumentException("Not a YYYY-MM-DD date: " + iso);
        }
        return new PasDate(Integer.parseInt(digits));
    }

    public int year() {
        return yyyymmdd / 10000;
    }

    public int month() {
        return (yyyymmdd / 100) % 100;
    }

    public int day() {
        return yyyymmdd % 100;
    }

    /** {@code ADD 10000 TO <date>} - what POLRNW does to produce the next term's expiry. */
    public PasDate plusYearsByArithmetic(int years) {
        return new PasDate(yyyymmdd + years * 10000);
    }

    /** What a calendar-aware implementation produces instead: 2024-02-29 + 1 year = 2025-02-28. */
    public PasDate plusYearsByCalendar(int years) {
        return of(toLocalDate().plusYears(years));
    }

    public boolean isRealCalendarDate() {
        try {
            LocalDate.of(year(), month(), day());
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    /** @throws DateTimeException if the eight digits are not a date, which is the point of this type. */
    public LocalDate toLocalDate() {
        return LocalDate.of(year(), month(), day());
    }

    @JsonValue
    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d", year(), month(), day());
    }

    @Override
    public int compareTo(PasDate other) {
        return Integer.compare(yyyymmdd, other.yyyymmdd);
    }
}
