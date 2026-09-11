package io.github.zhancm.repoonboard.web;

import io.github.zhancm.repoonboard.serialization.AnalysisReportJson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verify-phase check that reads the production UI from the packaged JAR itself. */
public final class PackagedUiVerifier {
    private static final String ROOT = "io/github/zhancm/repoonboard/web/ui/";
    private static final Pattern RESOURCE_REFERENCE = Pattern.compile(
            "(?:src|href)=\"([^\"]+)\"");

    private PackagedUiVerifier() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the packaged JAR path");
        }
        verify(Path.of(args[0]));
    }

    static void verify(Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
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
