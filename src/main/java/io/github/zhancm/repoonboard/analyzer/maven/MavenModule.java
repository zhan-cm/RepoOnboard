package io.github.zhancm.repoonboard.analyzer.maven;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Aggregation children are distinct from the module's declared Maven parent. */
public record MavenModule(
        String pomFileId,
        String baseDirectory,
        Optional<String> parentCoordinates,
        MavenProjectMetadata metadata,
        MavenMetadataValue sourceDirectory,
        List<MavenModule> children) {
    public MavenModule {
        Objects.requireNonNull(pomFileId, "pomFileId");
        Objects.requireNonNull(baseDirectory, "baseDirectory");
        Objects.requireNonNull(parentCoordinates, "parentCoordinates");
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(sourceDirectory, "sourceDirectory");
        children = List.copyOf(children);
    }
}
