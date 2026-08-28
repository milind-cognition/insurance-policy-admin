package com.acme.dropin.parity;

/** One case where the two implementations disagreed, with both documents kept. */
public record ParityDifference(String policyNumber, String operation, String mainframe, String mirror) {

    /**
     * What the difference is about, so a few hundred cases collapse into the
     * handful of root causes an audience can hold in their head.
     */
    public String category() {
        if (mainframe.startsWith("409") != mirror.startsWith("409")) {
            return "renewal eligibility";
        }
        if (!field(mainframe, "totalPremium").equals(field(mirror, "totalPremium"))) {
            return "premium rounding";
        }
        if (!field(mainframe, "expiryDate").equals(field(mirror, "expiryDate"))) {
            return "renewal date arithmetic";
        }
        return "other";
    }

    private static String field(String document, String name) {
        int at = document.indexOf('"' + name + '"');
        return at < 0 ? "" : document.substring(at, Math.min(document.length(), at + name.length() + 20));
    }

    /** The first place the two documents stop matching, which is usually the whole story. */
    public String firstDivergence() {
        int limit = Math.min(mainframe.length(), mirror.length());
        int index = 0;
        while (index < limit && mainframe.charAt(index) == mirror.charAt(index)) {
            index++;
        }
        int from = Math.max(0, index - 40);
        return "  mainframe: ..." + excerpt(mainframe, from) + System.lineSeparator()
                + "  mirror:    ..." + excerpt(mirror, from);
    }

    private static String excerpt(String document, int from) {
        return document.substring(from, Math.min(document.length(), from + 100));
    }

    @Override
    public String toString() {
        return policyNumber + " " + operation + System.lineSeparator() + firstDivergence();
    }
}
