package io.github.zhancm.repoonboard.web;

import io.github.zhancm.repoonboard.serialization.AnalysisReportJson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verify-phase check that exercises the actual V0.1 distribution JAR. */
public final class PackagedUiVerifier {
    private static final String MAIN_CLASS = "io.github.zhancm.repoonboard.cli.RepoOnboardCommand";
    private static final String ROOT = "io/github/zhancm/repoonboard/web/ui/";
    private static final Pattern RESOURCE_REFERENCE = Pattern.compile(
            "(?:src|href)=\"([^\"]+)\"");

    private PackagedUiVerifier() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected the packaged JAR and fixture paths");
        }
        Path jarPath = Path.of(args[0]);
        verify(jarPath);
        verifyOfflineAnalysis(jarPath, Path.of(args[1]));
        writeChecksum(jarPath);
    }

    static void verify(Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String mainClass = jar.getManifest().getMainAttributes()
                    .getValue(Attributes.Name.MAIN_CLASS);
            require(MAIN_CLASS.equals(mainClass),
                    "Packaged JAR does not declare the RepoOnboard CLI entry point");
            require(jar.getJarEntry("picocli/CommandLine.class") != null,
                    "Packaged JAR does not contain runtime dependencies");

            String index = read(jar, ROOT + "index.html");
            String script = read(jar, ROOT + "assets/app.js");
            String style = read(jar, ROOT + "assets/app.css");

            require(index.contains("<meta name=\"repoonboard-report-schema\" content=\""
                    + AnalysisReportJson.currentSchemaVersion() + "\""),
                    "Packaged index schema does not match the Java report schema");
            require(!index.contains("/src/main.js"),
                    "Packaged index still references the development entry point");
            require(script.contains("/api/report"),
                    "Packaged application does not contain the fixed report route");
            require(!script.isBlank() && !style.isBlank(),
                    "Packaged application assets must not be empty");

            Matcher references = RESOURCE_REFERENCE.matcher(index);
            int referenceCount = 0;
            while (references.find()) {
                String reference = references.group(1);
                require(reference.startsWith("/assets/")
                                && !reference.contains("://")
                                && jar.getJarEntry(ROOT + reference.substring(1)) != null,
                        "Packaged index contains a non-local or missing resource: " + reference);
                referenceCount++;
            }
            require(referenceCount == 2,
                    "Packaged index must reference exactly the local script and stylesheet");
        }
    }

    static void verifyOfflineAnalysis(Path jarPath, Path fixturePath) throws Exception {
        Path emptyRepository = Files.createTempDirectory("repoonboard-offline-repository-");
        Process process = null;
        StringBuffer output = new StringBuffer();
        try {
            process = new ProcessBuilder(
                    javaExecutable().toString(),
                    "-jar",
                    jarPath.toAbsolutePath().toString(),
                    fixturePath.toAbsolutePath().toString(),
                    "--local-repository",
                    emptyRepository.toString(),
                    "--no-open")
                    .redirectErrorStream(true)
                    .start();

            AtomicReference<URI> localUi = new AtomicReference<>();
            Process runningProcess = process;
            Thread reader = Thread.ofVirtual().start(() -> readOutput(runningProcess, output, localUi));
            long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
            while (localUi.get() == null && process.isAlive() && System.nanoTime() < deadline) {
                Thread.sleep(25);
            }

            require(localUi.get() != null,
                    "Executable JAR did not start the offline fixture UI. Output:\n" + output);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(localUi.get().resolve("api/report"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            require(response.statusCode() == 200,
                    "Executable JAR report route returned " + response.statusCode());
            require(response.body().contains("spring-analysis-project")
                            && response.body().contains("\"schemaVersion\""),
                    "Executable JAR did not serve the fixture analysis report");
            reader.join(Duration.ofSeconds(1));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
            }
            deleteTree(emptyRepository);
        }
    }

    static void writeChecksum(Path jarPath) throws IOException, NoSuchAlgorithmException {
        byte[] digest;
        try (var input = Files.newInputStream(jarPath)) {
            digest = MessageDigest.getInstance("SHA-256").digest(input.readAllBytes());
        }
        String line = HexFormat.of().formatHex(digest) + "  " + jarPath.getFileName() + "\n";
        Files.writeString(
                jarPath.resolveSibling(jarPath.getFileName() + ".sha256"),
                line,
                StandardCharsets.US_ASCII);
    }

    private static void readOutput(Process process, StringBuffer output, AtomicReference<URI> localUi) {
        try (var reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                if (line.startsWith("Local UI: ")) {
                    localUi.compareAndSet(null, URI.create(line.substring("Local UI: ".length())));
                }
            }
        } catch (IOException exception) {
            output.append("Could not read process output: ").append(exception.getMessage());
        }
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name").startsWith("Windows")
                ? "java.exe"
                : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String read(JarFile jar, String name) throws IOException {
        JarEntry entry = jar.getJarEntry(name);
        if (entry == null) {
            throw new IOException("Packaged UI resource is missing: " + name);
        }
        try (var input = jar.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
