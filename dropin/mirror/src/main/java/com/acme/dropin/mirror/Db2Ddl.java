package com.acme.dropin.mirror;

import com.acme.dropin.contract.RepoLayout;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns {@code sql/ddl/create-tables.sql} into DDL an embedded database will
 * accept, without editing the DDL.
 *
 * <p>Hand-copying the schema into a second file is the usual approach and it is
 * how the two drift apart. This reads the production DDL at build time and
 * makes exactly the changes z/OS-specific syntax requires:
 *
 * <ul>
 *   <li>{@code CREATE TABLESPACE} and the {@code IN ACMEDB.PASTS01} clause are
 *       storage directives with no embedded equivalent - dropped.</li>
 *   <li>{@code CURRENT TIMESTAMP} / {@code CURRENT DATE} become the SQL
 *       standard spellings with underscores.</li>
 *   <li>{@code CREATE SEQUENCE ... CACHE 50} uses DB2 spelling of options the
 *       inquiry and renewal paths never touch - dropped, and reported by
 *       {@link #droppedStatements()} rather than silently.</li>
 * </ul>
 *
 * <p>Column names, types, scales, keys and indexes are left exactly as DB2 has
 * them, which is the part the mirror has to get right.
 */
public final class Db2Ddl {

    private final List<String> statements = new ArrayList<>();
    private final List<String> dropped = new ArrayList<>();

    private Db2Ddl(String ddl) {
        for (String statement : stripComments(ddl).split(";")) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("COMMIT")) {
                continue;
            }
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("CREATE TABLESPACE") || upper.startsWith("CREATE SEQUENCE")) {
                dropped.add(firstLine(trimmed));
                continue;
            }
            statements.add(translate(trimmed));
        }
    }

    public static Db2Ddl fromRepository() {
        try {
            return new Db2Ddl(Files.readString(RepoLayout.repoRoot().resolve("sql/ddl/create-tables.sql")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Executable statements, in DDL order, prefixed with the schema creation the DDL assumes. */
    public List<String> statements() {
        List<String> all = new ArrayList<>();
        all.add("CREATE SCHEMA IF NOT EXISTS ACMEINS");
        all.addAll(statements);
        return List.copyOf(all);
    }

    /** DB2 storage syntax that has no embedded equivalent, named so it is not lost quietly. */
    public List<String> droppedStatements() {
        return List.copyOf(dropped);
    }

    private static String translate(String statement) {
        return statement
                .replaceAll("(?i)\\s+IN\\s+ACMEDB\\.[A-Z0-9_]+\\s*$", "")
                .replaceAll("(?i)\\s+IN\\s+ACMEDB\\s*$", "")
                .replaceAll("(?i)CURRENT TIMESTAMP", "CURRENT_TIMESTAMP")
                .replaceAll("(?i)CURRENT DATE", "CURRENT_DATE");
    }

    private static String stripComments(String ddl) {
        StringBuilder sql = new StringBuilder();
        for (String line : ddl.split("\n")) {
            if (!line.trim().startsWith("--")) {
                sql.append(line).append('\n');
            }
        }
        return sql.toString();
    }

    private static String firstLine(String statement) {
        return statement.lines().findFirst().orElse(statement).trim();
    }
}
