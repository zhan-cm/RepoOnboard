package io.github.zhancm.repoonboard.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class RepoOnboardCommandTest {

    @Test
    void resolvesCurrentDirectory() {
        CliResult result = execute(".");
        Path currentDirectory = Path.of(".").toAbsolutePath().normalize();

        assertEquals(CommandLine.ExitCode.OK, result.exitCode());
        assertTrue(result.out().contains("Target: " + currentDirectory));
        assertTrue(result.out().contains("repository analysis is not available yet"));
        assertTrue(result.err().isEmpty());
    }

    @Test
    void rejectsMissingPath(@TempDir Path temporaryDirectory) {
        Path missingPath = temporaryDirectory.resolve("missing");

        CliResult result = execute(missingPath.toString());

        assertEquals(CommandLine.ExitCode.USAGE, result.exitCode());
        assertTrue(result.err().contains("target path does not exist: " + missingPath));
    }

    @Test
    void rejectsFilePath(@TempDir Path temporaryDirectory) throws IOException {
        Path file = Files.createFile(temporaryDirectory.resolve("not-a-directory.txt"));

        CliResult result = execute(file.toString());

        assertEquals(CommandLine.ExitCode.USAGE, result.exitCode());
        assertTrue(result.err().contains("target path is not a directory: " + file));
    }

    @Test
    void printsHelp() {
        CliResult result = execute("--help");

        assertEquals(CommandLine.ExitCode.OK, result.exitCode());
        assertTrue(result.out().contains("Usage: repoonboard"));
        assertTrue(result.out().contains("PATH"));
        assertTrue(result.err().isEmpty());
    }

    @Test
    void printsVersion() {
        CliResult result = execute("--version");

        assertEquals(CommandLine.ExitCode.OK, result.exitCode());
        assertEquals("RepoOnboard 0.1.0-SNAPSHOT" + System.lineSeparator(), result.out());
        assertTrue(result.err().isEmpty());
    }

    @Test
    void requiresTargetPath() {
        CliResult result = execute();

        assertEquals(CommandLine.ExitCode.USAGE, result.exitCode());
        assertTrue(result.err().contains("Missing required parameter: 'PATH'"));
    }

    private static CliResult execute(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exitCode = RepoOnboardCommand.execute(
                args,
                new PrintWriter(out, true),
                new PrintWriter(err, true));
        return new CliResult(exitCode, out.toString(), err.toString());
    }

    private record CliResult(int exitCode, String out, String err) {
    }
}
