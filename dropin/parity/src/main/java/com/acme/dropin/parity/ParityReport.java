package com.acme.dropin.parity;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** The outcome of a parity run: how many cases matched, and what the rest looked like. */
public record ParityReport(int cases, List<ParityDifference> differences) {

    public int identical() {
        return cases - differences.size();
    }

    public boolean clean() {
        return differences.isEmpty();
    }

    public String headline() {
        return identical() + "/" + cases + " cases identical";
    }

    /** Differences grouped by what they are about, which is how they cluster. */
    public Map<String, List<ParityDifference>> byCategory() {
        return differences.stream().collect(Collectors.groupingBy(
                ParityDifference::category, TreeMap::new, Collectors.toList()));
    }

    public String render() {
        StringBuilder out = new StringBuilder();
        out.append(headline()).append(System.lineSeparator());
        if (clean()) {
            out.append("the mirror is a drop-in replacement for every case in the population")
                    .append(System.lineSeparator());
            return out.toString();
        }
        out.append(System.lineSeparator());
        byCategory().forEach((category, group) -> {
            out.append(group.size()).append(" cases: ").append(category)
                    .append(System.lineSeparator());
            group.stream().limit(2).forEach(difference ->
                    out.append("  ").append(difference.policyNumber())
                            .append(' ').append(difference.operation()).append(System.lineSeparator())
                            .append(difference.firstDivergence()).append(System.lineSeparator()));
            out.append(System.lineSeparator());
        });
        return out.toString();
    }
}
