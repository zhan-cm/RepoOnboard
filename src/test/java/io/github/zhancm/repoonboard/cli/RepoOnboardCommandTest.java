package io.github.zhancm.repoonboard.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class RepoOnboardCommandTest {

    @Test
    void resolvesCurrentDirectory() {
        CliResult result = execute(".");
        Path currentDirectory = Path.of(".").toAbsolutePath().normalize();

        assertEquals(CommandLine.ExitCode.SOFTWARE, result.exitCode());
        assertTrue(result.out().contains("Target: " + currentDirectory));
        assertTrue(result.out().contains("Maven project: detected (pom.xml)"));
        assertTrue(result.out().contains("groupId: io.github.zhancm"));
        assertTrue(result.out().contains("artifactId: repoonboard"));
        assertTrue(result.out().contains("version: 0.1.0-SNAPSHOT"));
        assertTrue(result.out().contains("packaging: jar"));
        assertTrue(result.out().contains("Metadata status: SUCCESS"));
        assertTrue(result.out().contains("Java source roots: 1"));
        assertTrue(result.out().contains("Java source files:"));
        assertTrue(result.out().contains("Java compilation units:"));
        assertTrue(result.out().contains("Java declarations:"));
        assertTrue(result.out().contains("Java declaration index:"));
        assertTrue(result.out().contains("Java type references:"));
        assertTrue(result.out().contains("Spring components: 0"));
        assertTrue(result.out().contains("Spring configurations: 0"));
        assertTrue(result.out().contains("Spring application entry points: 0"));
        assertTrue(result.out().contains("Spring injection candidates: 0"));
        assertTrue(result.out().contains("Spring MVC mapping declarations: 0"));
        assertTrue(result.out().contains("Spring HTTP endpoints: 0 (unresolved: 0)"));
        assertTrue(result.out().contains(
                "Spring component dependencies: 0 (confirmed: 0, unresolved: 0)"));
        assertTrue(result.err().contains(
                "ERROR [SPRING_BOOT_NOT_DETECTED] stage=SPRING_BOOT_DETECTION"));
    }

    @Test
    void reportsSpringMvcMappingDeclarations() {
        CliResult result = execute(FixturePaths.project("spring-api-project").toString());

        assertEquals(3, result.exitCode());
        assertTrue(result.out().contains("Spring MVC mapping declarations: 8"));
        assertTrue(result.out().contains("Spring HTTP endpoints: 17 (unresolved: 1)"));
        assertTrue(result.err().contains("SPRING_MVC_PATH_UNRESOLVED"));
        assertTrue(result.err().contains("SPRING_MVC_CONDITION_UNRESOLVED"));
    }

    @Test
    void carriesPipelineDiagnosticsAndPartialStatusIntoTheUiReport() {
        AtomicReference<AnalysisReport> launchedReport = new AtomicReference<>();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        int exitCode = RepoOnboardCommand.execute(
                new String[] {FixturePaths.project("spring-api-project").toString(), "--no-open"},
                new PrintWriter(out, true),
                new PrintWriter(err, true),
                (report, noOpen, standardOut, standardErr) -> launchedReport.set(report));

        assertEquals(3, exitCode);
        assertEquals(io.github.zhancm.repoonboard.core.model.AnalysisStatus.PARTIAL,
                launchedReport.get().status());
        assertTrue(launchedReport.get().diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("SPRING_MVC_PATH_UNRESOLVED")));
        assertTrue(launchedReport.get().diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("SPRING_MVC_CONDITION_UNRESOLVED")));
        var diagnostic = launchedReport.get().diagnostics().stream()
                .filter(item -> item.code().equals("SPRING_MVC_PATH_UNRESOLVED"))
                .findFirst()
                .orElseThrow();
        assertTrue(err.toString().contains(
                diagnostic.severity() + " [" + diagnostic.code() + "] stage=" + diagnostic.stage()));
        assertTrue(err.toString().contains("Message: " + diagnostic.message()));
        assertTrue(err.toString().contains("Source: " + diagnostic.fileId().orElseThrow()));
        assertTrue(err.toString().contains("Analysis result: PARTIAL"));
    }

    @Test
    void reportsConfirmedComponentDependencies() {
        CliResult result = execute(FixturePaths.project("spring-dependency-project").toString());

        assertEquals(3, result.exitCode());
        assertTrue(result.out().contains(
                "Spring component dependencies: 6 (confirmed: 5, unresolved: 1)"));
        assertTrue(result.err().contains("SPRING_DEPENDENCY_TARGET_UNRESOLVED"));
    }

    @Test
    void reportsConfirmedAndAmbiguousInjectionCandidates() {
        CliResult result = execute(FixturePaths.project("spring-injection-project").toString());

        assertEquals(3, result.exitCode());
        assertTrue(result.out().contains("Spring injection candidates: 5 (confirmed: 3, ambiguous: 2)"));
        assertTrue(result.err().contains("SPRING_CONSTRUCTOR_INJECTION_AMBIGUOUS"));
        assertTrue(result.err().contains("SPRING_METHOD_INJECTION_UNSUPPORTED"));
    }

    @Test
    void reportsSpringComponentKindsFromFixture() {
        CliResult result = execute(FixturePaths.project("spring-analysis-project").toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("Spring components: 6"));
        assertTrue(result.out().contains("CONTROLLER: 1"));
        assertTrue(result.out().contains("REST_CONTROLLER: 1"));
        assertTrue(result.out().contains("SERVICE: 1"));
        assertTrue(result.out().contains("REPOSITORY: 1"));
        assertTrue(result.out().contains("COMPONENT: 2"));
        assertTrue(result.out().contains("Spring configurations: 2"));
        assertTrue(result.out().contains("Spring application entry points: 1"));
    }

    @Test
    void reportsDiscoveredJavaFiles(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>app</artifactId><version>1</version><dependencies><dependency>
                <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId>
                <version>3.5.0</version></dependency></dependencies></project>
                """);
        Path source = directory.resolve("src/main/java/example");
        Files.createDirectories(source);
        Files.writeString(source.resolve("App.java"), "package example; class App {}");

        CliResult result = execute(directory.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("Java source roots: 1"));
        assertTrue(result.out().contains("src/main/java (pom.xml)"));
        assertTrue(result.out().contains("Java source files: 1"));
        assertTrue(result.out().contains("Java compilation units: 1"));
        assertTrue(result.out().contains("Java declarations: 1"));
        assertTrue(result.out().contains("Java declaration index: 1 entries"));
        assertTrue(result.out().contains("Java type references:"));
    }

    @Test
    void reportsANonMavenDirectoryClearly(@TempDir Path temporaryDirectory) throws IOException {
        Files.writeString(temporaryDirectory.resolve("README.md"), "unrelated");

        CliResult result = execute(temporaryDirectory.toString());

        assertEquals(CommandLine.ExitCode.SOFTWARE, result.exitCode());
        assertTrue(result.out().contains("Maven project: not detected"));
        assertTrue(result.out().contains("root pom.xml not found"));
        assertTrue(result.err().contains("ERROR [PROJECT_NOT_MAVEN] stage=PROJECT_DETECTION"));
        assertTrue(result.err().contains(
                "Action: Run RepoOnboard from the root of a Maven Spring Boot repository."));
    }

    @Test
    void rejectsMissingPath(@TempDir Path temporaryDirectory) {
        Path missingPath = temporaryDirectory.resolve("missing");

        CliResult result = execute(missingPath.toString());

        assertEquals(CommandLine.ExitCode.USAGE, result.exitCode());
        assertTrue(result.err().contains("ERROR [CLI_TARGET_NOT_FOUND] stage=CLI"));
        assertTrue(result.err().contains("Context: Target: " + missingPath));
        assertTrue(result.err().contains("The target path does not exist."));
    }

    @Test
    void rejectsFilePath(@TempDir Path temporaryDirectory) throws IOException {
        Path file = Files.createFile(temporaryDirectory.resolve("not-a-directory.txt"));

        CliResult result = execute(file.toString());

        assertEquals(CommandLine.ExitCode.USAGE, result.exitCode());
        assertTrue(result.err().contains("ERROR [CLI_TARGET_NOT_DIRECTORY] stage=CLI"));
        assertTrue(result.err().contains("Context: Target: " + file));
        assertTrue(result.err().contains("The target path is not a directory."));
    }

    @Test
    void printsHelp() {
        CliResult result = execute("--help");

        assertEquals(CommandLine.ExitCode.OK, result.exitCode());
        assertTrue(result.out().contains("Usage: repoonboard"));
        assertTrue(result.out().contains("PATH"));
        assertTrue(result.out().contains("--no-open"));
        assertTrue(result.err().isEmpty());
    }

    @Test
    void assemblesThePublicReportAndStartsTheUi() {
        AtomicReference<AnalysisReport> launchedReport = new AtomicReference<>();
        AtomicBoolean launchedWithoutBrowser = new AtomicBoolean();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        int exitCode = RepoOnboardCommand.execute(
                new String[] {
                    FixturePaths.project("spring-analysis-project").toString(), "--no-open"
                },
                new PrintWriter(out, true),
                new PrintWriter(err, true),
                (report, noOpen, standardOut, standardErr) -> {
                    launchedReport.set(report);
                    launchedWithoutBrowser.set(noOpen);
                });

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertEquals("spring-analysis-project", launchedReport.get().project().name());
        assertTrue(launchedReport.get().summary().componentCount() > 0);
        assertTrue(launchedWithoutBrowser.get());
    }

    @Test
    void reportsUiStartupFailure() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        int exitCode = RepoOnboardCommand.execute(
                new String[] {FixturePaths.project("minimal-maven-project").toString()},
                new PrintWriter(out, true),
                new PrintWriter(err, true),
                (report, noOpen, standardOut, standardErr) -> {
                    throw new IOException("port unavailable");
                });

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
        assertTrue(err.toString().contains("ERROR [LOCAL_UI_START_FAILED] stage=LOCAL_UI"));
        assertTrue(err.toString().contains("Action: Check local loopback networking"));
        assertFalse(err.toString().contains("port unavailable"));
    }

    @Test
    void rejectsAMavenProjectWithoutSpringBootAsUnsupported(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>plain-maven</artifactId><version>1</version></project>
                """);

        CliResult result = execute(directory.toString());

        assertEquals(CommandLine.ExitCode.SOFTWARE, result.exitCode());
        assertTrue(result.out().contains("Analysis status: FAILED"));
        assertTrue(result.err().contains(
                "ERROR [SPRING_BOOT_NOT_DETECTED] stage=SPRING_BOOT_DETECTION"));
        assertTrue(result.err().contains("Source: pom.xml"));
        assertTrue(result.err().contains("RepoOnboard V0.1 supports Spring Boot Maven repositories"));
    }

    @Test
    void keepsValidFactsAndReportsInvalidJavaSourceAsPartial(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>broken-source</artifactId><version>1</version><dependencies><dependency>
                <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId>
                <version>3.5.0</version></dependency></dependencies></project>
                """);
        Path source = directory.resolve("src/main/java/example");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Broken.java"), "package example; class Broken {");

        CliResult result = execute(directory.toString());

        assertEquals(3, result.exitCode());
        assertTrue(result.out().contains("Analysis status: PARTIAL"));
        assertTrue(result.err().contains("Analysis result: PARTIAL"));
        assertTrue(result.err().contains("WARNING [JAVA_PARSE_PROBLEM] stage=JAVA_PARSE"));
        assertTrue(result.err().contains("Source: src/main/java/example/Broken.java"));
        assertTrue(result.err().contains("analysis continued")
                || result.err().contains("confirmed facts were preserved"));
    }

    @Test
    void hidesUnexpectedExceptionDetailsAndReturnsFailedExitCode() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        int exitCode = RepoOnboardCommand.execute(
                new String[] {FixturePaths.project("spring-analysis-project").toString()},
                new PrintWriter(out, true),
                new PrintWriter(err, true),
                (report, noOpen, standardOut, standardErr) -> {
                    throw new IllegalStateException("secret-token-value");
                });

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
        assertTrue(err.toString().contains("ERROR [ANALYSIS_UNEXPECTED_FAILURE] stage=CLI"));
        assertTrue(err.toString().contains("Context: Failure type: IllegalStateException"));
        assertFalse(err.toString().contains("secret-token-value"));
    }

    @Test
    void printsSpringBootEvidenceAndUnknownVersion(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>app</artifactId><version>1</version><dependencies><dependency>
                <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId>
                <version>${unavailable}</version></dependency></dependencies></project>
                """);
        CliResult result = execute(directory.toString());
        assertEquals(3, result.exitCode());
        assertTrue(result.out().contains("Spring Boot build: detected; version: <unknown or conflicting>"));
        assertTrue(result.out().contains("DEPENDENCY spring-boot-starter-web (pom.xml)"));
        assertTrue(result.err().contains("SPRING_BOOT_VERSION_UNRESOLVED"));
    }

    @Test
    void printsModuleTreeAndPartialStatusForMissingChild(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>app</artifactId><version>1</version><packaging>pom</packaging>
                <modules><module>child</module><module>missing</module></modules></project>
                """);
        Path child = Files.createDirectory(directory.resolve("child"));
        Files.writeString(child.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>child</artifactId><version>1</version></project>
                """);
        CliResult result = execute(directory.toString());
        assertEquals(3, result.exitCode());
        assertTrue(result.out().contains("  child [child] source: child/src/main/java"));
        assertTrue(result.out().contains("Analysis status: PARTIAL"));
        assertTrue(result.err().contains("MAVEN_POM_SOURCE_UNAVAILABLE"));
    }

    @Test
    void selectsProfileAndReportsPartialAndFailedExitCodes(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>app</artifactId><version>${revision}</version>
                <dependencies><dependency><groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter</artifactId><version>3.5.0</version>
                </dependency></dependencies>
                <profiles><profile><id>manual</id><properties><revision>2</revision></properties>
                </profile></profiles></project>
                """);
        CliResult success = execute(directory.toString(), "--profile", "manual",
                "--local-repository", directory.resolve("empty-cache").toString());
        assertEquals(0, success.exitCode());
        assertTrue(success.out().contains("version: 2"));
        assertTrue(success.out().contains("Active profiles: [manual]"));
        CliResult partial = execute(directory.toString());
        assertEquals(3, partial.exitCode());
        assertTrue(partial.out().contains("PARTIAL"));
        assertTrue(partial.err().contains("MAVEN_METADATA_UNRESOLVED"));
        Files.writeString(directory.resolve("pom.xml"), "<project>");
        CliResult failed = execute(directory.toString());
        assertEquals(1, failed.exitCode());
        assertTrue(failed.err().contains("MAVEN_POM_XML_INVALID"));
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
        assertTrue(result.err().contains("ERROR [CLI_ARGUMENT_INVALID] stage=CLI"));
        assertTrue(result.err().contains("Missing required parameter: 'PATH'"));
        assertTrue(result.err().contains("Action: Run 'repoonboard --help'"));
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
