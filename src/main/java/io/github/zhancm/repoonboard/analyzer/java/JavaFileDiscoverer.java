package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Enumerates Java files under confirmed source roots without following links. */
public final class JavaFileDiscoverer {

    private static final String STAGE = "JAVA_FILES";
    private static final List<String> EXCLUDED_DIRECTORIES = List.of(
            "target", "generated-sources", "generated-test-sources", ".git", ".repoonboard");

    public JavaFileDiscovery discover(Path scanRoot, JavaSourceRootDiscovery sourceRoots) {
        Objects.requireNonNull(sourceRoots, "sourceRoots");
        if (scanRoot == null || !Files.isDirectory(scanRoot)) {
            throw new IllegalArgumentException("scanRoot must be an existing directory");
        }

        List<JavaSourceFile> files = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        final Path realScanRoot;
        try {
            realScanRoot = scanRoot.toRealPath();
        } catch (IOException exception) {
            diagnostics.add(diagnostic(
                    "JAVA_SCAN_ROOT_UNAVAILABLE", "pom.xml", DiagnosticSeverity.ERROR,
                    "The Java scan root could not be resolved safely."));
            return new JavaFileDiscovery(List.of(), diagnostics);
        }

        for (JavaSourceRoot sourceRoot : sourceRoots.sourceRoots()) {
            scan(realScanRoot, sourceRoot, files, diagnostics);
        }
        files.sort(Comparator.comparing(JavaSourceFile::modulePomFileId)
                .thenComparing(JavaSourceFile::relativePath));
        files = removeAmbiguousOwnership(files, diagnostics);
        return new JavaFileDiscovery(
                List.copyOf(new LinkedHashSet<>(files)),
                List.copyOf(new LinkedHashSet<>(diagnostics)));
    }

    private static List<JavaSourceFile> removeAmbiguousOwnership(
            List<JavaSourceFile> files, List<Diagnostic> diagnostics) {
        Map<String, JavaSourceFile> byPath = new LinkedHashMap<>();
        Set<String> ambiguous = new LinkedHashSet<>();
        for (JavaSourceFile file : files) {
            if (ambiguous.contains(file.relativePath())) {
                continue;
            }
            JavaSourceFile existing = byPath.putIfAbsent(file.relativePath(), file);
            if (existing != null
                    && !existing.modulePomFileId().equals(file.modulePomFileId())) {
                byPath.remove(file.relativePath());
                ambiguous.add(file.relativePath());
                diagnostics.add(diagnostic(
                        "JAVA_FILE_MODULE_OWNERSHIP_AMBIGUOUS",
                        file.relativePath(),
                        DiagnosticSeverity.WARNING,
                        "A Java file belongs to overlapping source roots from multiple modules."));
            }
        }
        return new ArrayList<>(byPath.values());
    }

    private void scan(
            Path scanRoot,
            JavaSourceRoot sourceRoot,
            List<JavaSourceFile> files,
            List<Diagnostic> diagnostics) {
        Path configuredRoot = scanRoot.resolve(sourceRoot.relativePath()).normalize();
        try {
            if (!Files.isDirectory(configuredRoot)) {
                diagnostics.add(diagnostic(
                        "JAVA_SOURCE_ROOT_UNAVAILABLE",
                        sourceRoot.modulePomFileId(),
                        DiagnosticSeverity.WARNING,
                        "A discovered Java source root is no longer available."));
                return;
            }
            Path realSourceRoot = configuredRoot.toRealPath();
            if (!realSourceRoot.startsWith(scanRoot)) {
                diagnostics.add(diagnostic(
                        "JAVA_SOURCE_ROOT_OUTSIDE_SCAN",
                        sourceRoot.modulePomFileId(),
                        DiagnosticSeverity.WARNING,
                        "A Java source root resolves outside the scan root."));
                return;
            }
            Files.walkFileTree(realSourceRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    if (!directory.equals(realSourceRoot)
                            && (attributes.isSymbolicLink() || isExcluded(directory.getFileName()))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (attributes.isRegularFile() && file.getFileName().toString().endsWith(".java")) {
                        inspectFile(scanRoot, realSourceRoot, sourceRoot, file, files, diagnostics);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) {
                    diagnostics.add(diagnostic(
                            "JAVA_FILE_UNAVAILABLE",
                            relativeOrModule(scanRoot, file, sourceRoot),
                            DiagnosticSeverity.WARNING,
                            "A Java source path could not be inspected."));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | RuntimeException exception) {
            diagnostics.add(diagnostic(
                    "JAVA_SOURCE_ROOT_SCAN_FAILED",
                    sourceRoot.modulePomFileId(),
                    DiagnosticSeverity.WARNING,
                    "A Java source root could not be scanned completely."));
        }
    }

    private static void inspectFile(
            Path scanRoot,
            Path sourceRoot,
            JavaSourceRoot ownership,
            Path file,
            List<JavaSourceFile> files,
            List<Diagnostic> diagnostics) {
        try {
            Path realFile = file.toRealPath();
            if (!realFile.startsWith(scanRoot) || !realFile.startsWith(sourceRoot)) {
                diagnostics.add(diagnostic(
                        "JAVA_FILE_OUTSIDE_SOURCE_ROOT",
                        ownership.modulePomFileId(),
                        DiagnosticSeverity.WARNING,
                        "A Java file resolves outside its confirmed source root."));
                return;
            }
            String relativePath = relative(scanRoot, realFile);
            files.add(new JavaSourceFile(
                    ownership.modulePomFileId(),
                    ownership.moduleBaseDirectory(),
                    ownership.relativePath(),
                    relativePath,
                    SourceLocation.file(relativePath)));
        } catch (IOException | RuntimeException exception) {
            diagnostics.add(diagnostic(
                    "JAVA_FILE_UNAVAILABLE",
                    ownership.modulePomFileId(),
                    DiagnosticSeverity.WARNING,
                    "A Java source file could not be resolved safely."));
        }
    }

    private static boolean isExcluded(Path name) {
        return name != null && EXCLUDED_DIRECTORIES.contains(name.toString());
    }

    private static String relativeOrModule(Path root, Path path, JavaSourceRoot ownership) {
        try {
            Path normalized = path.toAbsolutePath().normalize();
            return normalized.startsWith(root) ? relative(root, normalized) : ownership.modulePomFileId();
        } catch (RuntimeException exception) {
            return ownership.modulePomFileId();
        }
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static Diagnostic diagnostic(
            String code, String sourceFileId, DiagnosticSeverity severity, String message) {
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
