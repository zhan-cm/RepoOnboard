package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Resolved project metadata produced without executing or networking through Maven. */
public record MavenProjectMetadata(
        MavenMetadataValue groupId,
        MavenMetadataValue artifactId,
        MavenMetadataValue version,
        MavenMetadataValue packaging,
        Map<String, MavenMetadataValue> properties,
        List<String> activeProfileIds,
        List<String> sourcePomIds,
        List<Diagnostic> diagnostics) {

    public MavenProjectMetadata {
        groupId = Objects.requireNonNull(groupId, "groupId");
        artifactId = Objects.requireNonNull(artifactId, "artifactId");
        version = Objects.requireNonNull(version, "version");
        packaging = Objects.requireNonNull(packaging, "packaging");
        properties = Map.copyOf(Objects.requireNonNull(properties, "properties"));
        activeProfileIds = List.copyOf(
                Objects.requireNonNull(activeProfileIds, "activeProfileIds"));
        sourcePomIds = List.copyOf(Objects.requireNonNull(sourcePomIds, "sourcePomIds"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public MavenProjectMetadata(
            MavenMetadataValue groupId,
            MavenMetadataValue artifactId,
            MavenMetadataValue version,
            MavenMetadataValue packaging,
            List<Diagnostic> diagnostics) {
        this(
                groupId,
                artifactId,
                version,
                packaging,
                Map.of(),
                List.of(),
                List.of("pom.xml"),
                diagnostics);
    }

    public Optional<MavenMetadataValue> property(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(properties.get(name));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
