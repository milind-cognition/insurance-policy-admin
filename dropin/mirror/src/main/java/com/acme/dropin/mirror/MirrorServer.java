package com.acme.dropin.mirror;

import com.acme.dropin.contract.RepoLayout;
import org.h2.tools.Server;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Runs the mirror's database as a standalone server so a process that is not
 * this one can read it.
 *
 * <p>Specifically: the incumbent Spring Boot 1.5 facade, started from its own
 * built jar with no source change, pointed here with a datasource URL. It
 * cannot tell that the rows it is reading are served by the mirror's schema
 * rather than by its own local database - which is the drop-in claim tested on
 * the incumbent binary itself rather than on a reimplementation of it.
 *
 * <p>Optionally seeds from a SQL script, so the facade's own fixtures can be
 * loaded into a schema translated from the production DDL - if the incumbent's
 * seed data does not load, the schema is wrong, and that shows up here.
 */
public final class MirrorServer {

    private MirrorServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(argument(args, "--port", "9092"));
        Path database = Path.of(argument(args, "--database",
                RepoLayout.repoRoot().resolve("dropin/target/pas-mirror").toString()));
        String seed = argument(args, "--seed", "");

        Files.createDirectories(database.getParent());
        Server server = Server.createTcpServer("-tcpPort", String.valueOf(port), "-tcpAllowOthers",
                "-ifNotExists").start();

        String url = "jdbc:h2:" + database.toAbsolutePath() + ";MODE=DB2";
        MirrorDatabase mirror = MirrorDatabase.of(url);
        if (seed.isEmpty()) {
            mirror.loadDemoData();
        } else {
            try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(seed));
            }
        }

        System.out.println("mirror database ready on jdbc:h2:tcp://localhost:" + port + "/"
                + database.toAbsolutePath() + ";MODE=DB2");
        System.out.flush();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        Thread.currentThread().join();
    }

    private static String argument(String[] args, String name, String fallback) {
        for (String arg : args) {
            if (arg.startsWith(name + "=")) {
                return arg.substring(name.length() + 1);
            }
        }
        return fallback;
    }
}
