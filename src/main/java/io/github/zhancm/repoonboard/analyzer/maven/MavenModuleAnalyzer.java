package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.apache.maven.model.Build;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

/** Walks declared aggregation only, with a shared snapshot repository for all POMs. */
public final class MavenModuleAnalyzer {
    public MavenModuleAnalysis analyze(Path scanRoot, MavenModelOptions options) {
        if (scanRoot == null || !Files.isDirectory(scanRoot)) {
            throw new IllegalArgumentException("scanRoot must be an existing directory");
        }
        List<Diagnostic> diagnostics = new ArrayList<>();
        try {
            Path root = scanRoot.toRealPath();
            RestrictedPomRepository repository = new RestrictedPomRepository(root, options);
            MavenModule module = visit(root, repository.rootSource(), repository, options,
                    new LinkedHashSet<>(), diagnostics);
            for (String requested : options.activeProfileIds()) {
                boolean found = repository.sources().stream().flatMap(pom -> pom.rawModel().getProfiles().stream())
                        .anyMatch(profile -> requested.equals(profile.getId()));
                if (!found) {
                    diagnostics.add(diagnostic("MAVEN_PROFILE_NOT_FOUND", "pom.xml", DiagnosticSeverity.WARNING,
                            "An explicitly requested Maven profile was not found: " + requested));
                }
            }
            return new MavenModuleAnalysis(Optional.of(module), List.copyOf(new LinkedHashSet<>(diagnostics)));
        } catch (IOException | RestrictedPomException exception) {
            String code = exception instanceof RestrictedPomException problem
                    ? problem.code() : "MAVEN_SCAN_ROOT_UNAVAILABLE";
            diagnostics.add(diagnostic(code, "pom.xml", DiagnosticSeverity.ERROR,
                    "The root POM could not be read from the allowed scan root."));
            return new MavenModuleAnalysis(Optional.empty(), diagnostics);
        }
    }

    private MavenModule visit(Path scanRoot, RestrictedPomSource source,
            RestrictedPomRepository repository, MavenModelOptions options, Set<Path> visited,
            List<Diagnostic> diagnostics) {
        visited.add(source.realFile());
        var built = MavenProjectMetadataReader.buildSource(repository, source, options);
        diagnostics.addAll(built.metadata().diagnostics());
        Model usable = built.effective() == null ? built.raw() : built.effective();
        MavenMetadataValue sourceDirectory = sourceDirectory(scanRoot, source, repository,
                built, diagnostics);
        List<String> declarations = new ArrayList<>(usable.getModules());
        if (built.effective() == null) {
            for (var profile : built.raw().getProfiles()) {
                if (built.metadata().activeProfileIds().contains(profile.getId())) {
                    declarations.addAll(profile.getModules());
                }
            }
        }
        List<MavenModule> children = new ArrayList<>();
        for (String declaration : declarations) {
            String value = interpolate(declaration, built.metadata(), source.realFile().getParent());
            try {
                if (value.isBlank() || value.contains("${")) {
                    throw new IllegalArgumentException("Unresolved module path");
                }
                Path modulePath = safePath(scanRoot, source.realFile().getParent().resolve(value));
                Path pom = Files.isDirectory(modulePath) ? modulePath.resolve("pom.xml") : modulePath;
                RestrictedPomSource child = repository.moduleSource(pom);
                if (visited.contains(child.realFile())) {
                    diagnostics.add(diagnostic("MAVEN_MODULE_REPEATED", source.sourceFileId(),
                            DiagnosticSeverity.WARNING, "A duplicate or cyclic module declaration was omitted."));
                    continue;
                }
                children.add(visit(scanRoot, child, repository, options, visited, diagnostics));
            } catch (RestrictedPomException exception) {
                diagnostics.add(diagnostic(exception.code(), source.sourceFileId(),
                        DiagnosticSeverity.WARNING, "A declared module POM could not be read safely."));
            } catch (IOException | IllegalArgumentException exception) {
                diagnostics.add(diagnostic("MAVEN_MODULE_PATH_REJECTED", source.sourceFileId(),
                        DiagnosticSeverity.WARNING, "A module path is missing, unresolved, or outside the scan root."));
            }
        }
        Parent parent = usable.getParent();
        Optional<String> parentCoordinates = parent == null ? Optional.empty()
                : Optional.of(parent.getGroupId() + ":" + parent.getArtifactId() + ":" + parent.getVersion());
        var dependencies = new MavenDependencyExtractor().extract(built, source, repository, diagnostics);
        var springBoot = new SpringBootBuildDetector().detect(built, source, dependencies);
        if (springBoot.detected() && springBoot.version().isEmpty()) {
            diagnostics.add(diagnostic("SPRING_BOOT_VERSION_UNRESOLVED", source.sourceFileId(),
                    DiagnosticSeverity.WARNING, "Spring Boot build evidence exists, but its version is unknown or conflicting."));
        }
        return new MavenModule(source.sourceFileId(), relative(scanRoot, source.realFile().getParent()),
                parentCoordinates, built.metadata(), sourceDirectory,
                dependencies, springBoot, children);
    }

    private MavenMetadataValue sourceDirectory(Path scanRoot, RestrictedPomSource source,
            RestrictedPomRepository repository, MavenProjectMetadataReader.BuiltModel built,
            List<Diagnostic> diagnostics) {
        Build build = built.effective() == null ? built.raw().getBuild() : built.effective().getBuild();
        InputLocation location = build == null ? null : build.getLocation("sourceDirectory");
        String originId = source.sourceFileId();
        if (location != null && location.getSource() != null
                && repository.recognizesSource(location.getSource().getLocation())) {
            originId = location.getSource().getLocation();
        }
        Model originModel = repository.sourceById(originId).orElseThrow().rawModel();
        Optional<String> raw = Optional.ofNullable(originModel.getBuild())
                .map(Build::getSourceDirectory).filter(value -> !value.isBlank());
        String value = build == null ? null : build.getSourceDirectory();
        if (value == null) {
            value = "src/main/java";
        }
        value = interpolate(value, built.metadata(), source.realFile().getParent());
        SourceLocation origin = new SourceLocation(originId,
                positive(location == null ? 0 : location.getLineNumber()),
                positive(location == null ? 0 : location.getColumnNumber()),
                OptionalInt.empty(), OptionalInt.empty(), Optional.of("project.build.sourceDirectory"));
        try {
            if (value.contains("${")) {
                throw new IllegalArgumentException("Unresolved source directory");
            }
            Path directory = safePath(scanRoot, source.realFile().getParent().resolve(value));
            return new MavenMetadataValue(raw, Optional.of(relative(scanRoot, directory)),
                    MavenResolutionStatus.RESOLVED, origin);
        } catch (IOException | IllegalArgumentException exception) {
            diagnostics.add(diagnostic("MAVEN_SOURCE_DIRECTORY_REJECTED", source.sourceFileId(),
                    DiagnosticSeverity.WARNING, "The source directory is unresolved or outside the scan root."));
            return new MavenMetadataValue(raw, Optional.empty(),
                    raw.isPresent() ? MavenResolutionStatus.UNRESOLVED : MavenResolutionStatus.MISSING, origin);
        }
    }

    private static String interpolate(String value, MavenProjectMetadata metadata, Path baseDirectory) {
        String result = value.replace("${project.basedir}", baseDirectory.toString())
                .replace("${basedir}", baseDirectory.toString());
        for (int i = 0; i < 64 && result.contains("${"); i++) {
            String before = result;
            for (var property : metadata.properties().entrySet()) {
                if (property.getValue().resolvedValue().isPresent()) {
                    result = result.replace("${" + property.getKey() + "}",
                            property.getValue().resolvedValue().orElseThrow());
                }
            }
            if (before.equals(result) || result.length() > 65_536) {
                break;
            }
        }
        return result;
    }

    /** Resolves existing ancestors too, so a missing descendant cannot hide an escaping symlink. */
    private static Path safePath(Path scanRoot, Path candidate) throws IOException {
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(scanRoot)) {
            throw new IOException("Outside scan root");
        }
        Path ancestor = normalized;
        while (!Files.exists(ancestor)) {
            ancestor = ancestor.getParent();
        }
        Path real = ancestor.toRealPath().resolve(ancestor.relativize(normalized)).normalize();
        if (!real.startsWith(scanRoot)) {
            throw new IOException("Outside scan root");
        }
        return real;
    }

    private static String relative(Path root, Path path) {
        String value = root.relativize(path).toString().replace('\\', '/');
        return value.isEmpty() ? "." : value;
    }

    private static OptionalInt positive(int value) {
        return value > 0 ? OptionalInt.of(value) : OptionalInt.empty();
    }

    private static Diagnostic diagnostic(String code, String source, DiagnosticSeverity severity, String message) {
        return new Diagnostic(code, severity, "MAVEN_MODULES", Optional.empty(), Optional.of(source),
                Optional.of(SourceLocation.file(source)), message);
    }
}
