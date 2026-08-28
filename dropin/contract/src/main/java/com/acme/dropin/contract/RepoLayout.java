package com.acme.dropin.contract;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Locates the incumbent sources this contract is derived from. */
public final class RepoLayout {

    private RepoLayout() {
    }

    /** Repository root, found by walking up from the working directory. */
    public static Path repoRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("cobol/copybooks"))
                    && Files.isDirectory(dir.resolve("java-facade"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "Cannot locate repository root from " + Paths.get("").toAbsolutePath());
    }

    public static Path copybook(String name) {
        return repoRoot().resolve("cobol/copybooks").resolve(name);
    }

    public static Path cobolProgram(String name) {
        return repoRoot().resolve("cobol/programs").resolve(name);
    }

    public static Path facadeSource(String classFqn) {
        return repoRoot()
                .resolve("java-facade/src/main/java")
                .resolve(classFqn.replace('.', '/') + ".java");
    }
}
