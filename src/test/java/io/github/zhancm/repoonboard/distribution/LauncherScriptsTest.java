package io.github.zhancm.repoonboard.distribution;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LauncherScriptsTest {
    private final Path repository = Path.of(System.getProperty("user.dir"));

    @Test
    void windowsLauncherChecksRuntimeLocatesJarAndForwardsArguments() throws IOException {
        String launcher = Files.readString(repository.resolve("repoonboard.cmd"));

        assertTrue(launcher.contains("Java 21 or newer"));
        assertTrue(launcher.contains("%~dp0repoonboard.jar"));
        assertTrue(launcher.contains("%~dp0target\\repoonboard.jar"));
        assertTrue(launcher.contains("%*"));
    }

    @Test
    void posixLauncherChecksRuntimeLocatesJarAndForwardsArguments() throws IOException {
        String launcher = Files.readString(repository.resolve("repoonboard"));

        assertTrue(launcher.startsWith("#!/bin/sh"));
        assertTrue(launcher.contains("Java 21 or newer"));
        assertTrue(launcher.contains("$SCRIPT_DIR/repoonboard.jar"));
        assertTrue(launcher.contains("$SCRIPT_DIR/target/repoonboard.jar"));
        assertTrue(launcher.contains("\"$@\""));
    }
}
