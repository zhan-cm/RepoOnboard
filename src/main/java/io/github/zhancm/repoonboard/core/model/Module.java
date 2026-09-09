package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A build module with only analyzer-neutral metadata. */
public record Module(
        String id,
        String pomFileId,
        String baseDirectory,
        Optional<String> groupId,
        Optional<String> artifactId,
        Optional<String> version,
        Optional<String> packaging,
        List<String> sourceRoots,
        List<Framework> frameworks,
        List<Evidence> evidence) {
    public Module {
        id = ModelValues.requireText(id, "id");
        pomFileId = SourceLocation.file(pomFileId).sourceFileId();
        baseDirectory = ModelValues.requireRelativeDirectory(baseDirectory, "baseDirectory");
        groupId = ModelValues.requireOptionalText(groupId, "groupId");
        artifactId = ModelValues.requireOptionalText(artifactId, "artifactId");
        version = ModelValues.requireOptionalText(version, "version");
        packaging = ModelValues.requireOptionalText(packaging, "packaging");
        sourceRoots = Objects.requireNonNull(sourceRoots, "sourceRoots").stream()
                .distinct().sorted().toList();
        frameworks = Objects.requireNonNull(frameworks, "frameworks").stream()
                .distinct().sorted().toList();
        evidence = ModelValues.stableEvidence(evidence);
    }
}
