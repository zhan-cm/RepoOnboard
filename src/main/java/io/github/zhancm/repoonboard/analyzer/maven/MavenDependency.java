package io.github.zhancm.repoonboard.analyzer.maven;

import java.util.Objects;

/** A declared or inherited dependency; this is not a transitive dependency graph. */
public record MavenDependency(MavenMetadataValue groupId, MavenMetadataValue artifactId,
        MavenMetadataValue version, MavenMetadataValue scope) {
    public MavenDependency {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(artifactId, "artifactId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(scope, "scope");
    }
}
