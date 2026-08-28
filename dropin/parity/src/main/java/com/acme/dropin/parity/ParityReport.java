package com.acme.dropin.parity;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The outcome of a parity run: how many cases matched, and what the rest looked
 * like.
 *
 * <p>A difference can be <em>waived</em>: a divergence someone has looked at,
 * decided is correct, and written down. Waivers are named by category through
 * {@code -Dpas.parity.waived=...} and listed in dropin/docs. Everything not
 * waived is a defect and fails the run - a waiver is a decision on the record,
 * not a way to make the gate quiet.
 */
public record ParityReport(int cases, List<ParityDifference> differences, List<ParityDifference> waived) {

    public ParityReport(int cases, List<ParityDifference> differences) {
        this(cases, differences, List.of());
    }

    public int identical() {
        return cases - differences.size() - waived.size();
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

    public Map<String, List<ParityDifference>> waivedByCategory() {
        return waived.stream().collect(Collectors.groupingBy(
                ParityDifference::category, TreeMap::new, Collectors.toList()));
    }

    public String render() {
        StringBuilder out = new StringBuilder();
        out.append(headline()).append(System.lineSeparator());
        if (!waived.isEmpty()) {
            waivedByCategory().forEach((category, group) -> out
                    .append(group.size()).append(" cases waived: ").append(category)
                    .append(" (see dropin/docs/REFERENCE-SOLUTION.md)").append(System.lineSeparator()));
        }
        if (clean()) {
            out.append(waived.isEmpty()
                            ? "the mirror is a drop-in replacement for every case in the population"
                            : "no unexplained differences: every other case is byte-identical")
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
