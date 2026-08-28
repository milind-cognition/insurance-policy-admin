package com.acme.dropin.contract;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A record layout parsed straight out of a copybook in {@code cobol/copybooks}.
 *
 * <p>Nothing here is transcribed by hand: field names, order, PIC clauses and
 * COMP-3 scale are read from the copybook the CICS programs actually compile
 * against, so the layout in this repository and the layout a caller sees can
 * never drift apart without a test failing.
 *
 * <p>Level-88 condition names are recorded but do not consume storage. Group
 * items (a level with no PIC, e.g. {@code CUST-ADDRESS}) contribute their
 * subordinate elementary items only.
 */
public final class Copybook {

    /** COBOL fixed-format: columns 1-6 are sequence numbers, column 7 is the indicator. */
    private static final int INDICATOR_COLUMN = 6;

    private static final Pattern FIELD = Pattern.compile(
            "^\\s*(\\d{2})\\s+([A-Z0-9-]+)\\s*(?:PIC(?:TURE)?\\s+([^\\s.]+))?\\s*(COMP-3|COMP-5|COMP)?\\s*\\.?\\s*$");
    private static final Pattern CONDITION = Pattern.compile(
            "^\\s*88\\s+([A-Z0-9-]+)\\s+VALUE\\s+'?([^'.]*)'?\\s*\\.?\\s*$");

    private final String recordName;
    private final List<CopybookField> fields;
    private final Map<String, List<String>> conditionNames;

    private Copybook(String recordName, List<CopybookField> fields, Map<String, List<String>> conditionNames) {
        this.recordName = recordName;
        this.fields = List.copyOf(fields);
        this.conditionNames = Map.copyOf(conditionNames);
    }

    public static Copybook parse(Path copybook) {
        try {
            return parse(Files.readString(copybook, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read copybook " + copybook, e);
        }
    }

    public static Copybook parse(String source) {
        String recordName = null;
        List<CopybookField> fields = new ArrayList<>();
        Map<String, List<String>> conditions = new LinkedHashMap<>();
        int offset = 0;
        String lastElementary = null;

        for (String rawLine : source.split("\\R")) {
            if (rawLine.length() > INDICATOR_COLUMN && rawLine.charAt(INDICATOR_COLUMN) == '*') {
                continue;
            }
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher condition = CONDITION.matcher(line);
            if (condition.matches()) {
                if (lastElementary != null) {
                    conditions.computeIfAbsent(lastElementary, k -> new ArrayList<>()).add(condition.group(2));
                }
                continue;
            }
            Matcher field = FIELD.matcher(line);
            if (!field.matches()) {
                throw new IllegalArgumentException("Unparsable copybook line: " + rawLine);
            }
            int level = Integer.parseInt(field.group(1));
            String name = field.group(2);
            String pic = field.group(3);
            String usageToken = field.group(4);

            if (level == 1) {
                recordName = name;
                continue;
            }
            if (pic == null) {
                // Group item: its subordinates carry the storage.
                continue;
            }
            PicClause.Usage usage = usageToken == null ? PicClause.Usage.DISPLAY
                    : usageToken.startsWith("COMP-3") ? PicClause.Usage.COMP_3 : PicClause.Usage.COMP;
            CopybookField parsed = new CopybookField(name, level, PicClause.parse(pic, usage), offset);
            fields.add(parsed);
            offset += parsed.length();
            lastElementary = name;
        }
        if (recordName == null) {
            throw new IllegalArgumentException("No 01-level record found in copybook");
        }
        return new Copybook(recordName, fields, conditions);
    }

    public String recordName() {
        return recordName;
    }

    public List<CopybookField> fields() {
        return fields;
    }

    public CopybookField field(String name) {
        return fields.stream()
                .filter(f -> f.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No field " + name + " in " + recordName));
    }

    /** Level-88 condition values declared against the given field. */
    public List<String> conditionValues(String fieldName) {
        return conditionNames.getOrDefault(fieldName, List.of());
    }

    /** Total byte length of the record image, FILLER included. */
    public int recordLength() {
        return fields.stream().mapToInt(CopybookField::end).max().orElse(0);
    }

    /** Human readable field table, used by CONTRACT.md and the contract dump tool. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-28s %-6s %-6s %-26s%n", "FIELD", "OFFSET", "LEN", "PICTURE"));
        for (CopybookField f : fields) {
            sb.append(String.format("%-28s %-6d %-6d %-26s%n",
                    f.name(), f.offset(), f.length(), f.pic()));
        }
        sb.append(String.format("%-28s %-6s %-6d%n", "(record length)", "", recordLength()));
        return sb.toString();
    }
}
