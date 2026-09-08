package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Basic coordinates and packaging read from one root Maven POM. */
public record MavenProjectMetadata(
        MavenMetadataValue groupId,
        MavenMetadataValue artifactId,
        MavenMetadataValue version,
        MavenMetadataValue packaging,
        List<Diagnostic> diagnostics) {

    public MavenProjectMetadata {
        groupId = Objects.requireNonNull(groupId, "groupId");
        artifactId = Objects.requireNonNull(artifactId, "artifactId");
        version = Objects.requireNonNull(version, "version");
        packaging = Objects.requireNonNull(packaging, "packaging");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
