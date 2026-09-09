package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalysis;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Discovers existing Java main-source roots without recursively scanning the repository. */
public final class JavaSourceRootDiscoverer {

    private static final String STAGE = "JAVA_SOURCE_ROOTS";
    private static final List<String> EXCLUDED_SEGMENTS = List.of(
            "target", "generated-sources", "generated-test-sources", ".git", ".repoonboard");

    public JavaSourceRootDiscovery discover(Path scanRoot, MavenModuleAnalysis mavenAnalysis) {
        Objects.requireNonNull(mavenAnalysis, "mavenAnalysis");
        if (scanRoot == null || !Files.isDirectory(scanRoot)) {
            throw new IllegalArgumentException("scanRoot must be an existing directory");
        }

        List<JavaSourceRoot> roots = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        try {
            Path realRoot = scanRoot.toRealPath();
            mavenAnalysis.root().ifPresent(module -> collect(realRoot, module, roots, diagnostics));
        } catch (IOException exception) {
            diagnostics.add(diagnostic(
                    "JAVA_SCAN_ROOT_UNAVAILABLE",
                    "pom.xml",
                    DiagnosticSeverity.ERROR,
                    "The Java scan root could not be resolved safely."));
        }
        return new JavaSourceRootDiscovery(
                List.copyOf(new LinkedHashSet<>(roots)),
                List.copyOf(new LinkedHashSet<>(diagnostics)));
    }

    private void collect(
            Path scanRoot,
            MavenModule module,
            List<JavaSourceRoot> roots,
            List<Diagnostic> diagnostics) {
        module.sourceDirectory().resolvedValue().ifPresent(relativePath -> {
            if (isExcluded(relativePath)) {
                diagnostics.add(diagnostic(
                        "JAVA_SOURCE_ROOT_EXCLUDED",
                        module.pomFileId(),
                        DiagnosticSeverity.WARNING,
                        "A Java source root under generated or build output was excluded."));
            } else {
                inspect(scanRoot, module, relativePath, roots, diagnostics);
            }
        });
        module.children().forEach(child -> collect(scanRoot, child, roots, diagnostics));
    }

    private void inspect(
            Path scanRoot,
            MavenModule module,
            String relativePath,
            List<JavaSourceRoot> roots,
            List<Diagnostic> diagnostics) {
        try {
            Path sourceRoot = scanRoot.resolve(relativePath).normalize();
            if (!sourceRoot.startsWith(scanRoot) || !Files.exists(sourceRoot)) {
                return;
            }
            Path realSourceRoot = sourceRoot.toRealPath();
            if (!realSourceRoot.startsWith(scanRoot)) {
                diagnostics.add(diagnostic(
                        "JAVA_SOURCE_ROOT_OUTSIDE_SCAN",
                        module.pomFileId(),
                        DiagnosticSeverity.WARNING,
                        "A Java source root resolves outside the scan root."));
            } else if (!Files.isDirectory(realSourceRoot)) {
                diagnostics.add(diagnostic(
                        "JAVA_SOURCE_ROOT_NOT_DIRECTORY",
                        module.pomFileId(),
                        DiagnosticSeverity.WARNING,
                        "A configured Java source root is not a directory."));
            } else {
                String normalized = scanRoot.relativize(realSourceRoot)
                        .toString()
                        .replace('\\', '/');
                if (normalized.isEmpty()) {
                    normalized = ".";
                }
                roots.add(new JavaSourceRoot(
                        module.pomFileId(),
                        module.baseDirectory(),
                        normalized,
                        module.sourceDirectory().origin()));
            }
        } catch (IOException | RuntimeException exception) {
            diagnostics.add(diagnostic(
                    "JAVA_SOURCE_ROOT_UNAVAILABLE",
                    module.pomFileId(),
                    DiagnosticSeverity.WARNING,
                    "A configured Java source root could not be inspected safely."));
        }
    }

    private static boolean isExcluded(String relativePath) {
        for (String segment : relativePath.replace('\\', '/').split("/")) {
            if (EXCLUDED_SEGMENTS.contains(segment)) {
                return true;
            }
        }
        return false;
    }

    private static Diagnostic diagnostic(
            String code,
            String sourceFileId,
            DiagnosticSeverity severity,
            String message) {
        return new Diagnostic(
                code,
                severity,
                STAGE,
                Optional.empty(),
                Optional.of(sourceFileId),
                Optional.of(SourceLocation.file(sourceFileId)),
                message);
    }
}
