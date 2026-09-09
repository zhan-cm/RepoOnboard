package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Build evidence indicates Spring Boot usage, not the existence of an application entry point. */
public record SpringBootBuild(List<Signal> evidence) {
    public SpringBootBuild {
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
    }

    public boolean detected() {
        return !evidence.isEmpty();
    }

    public Optional<String> version() {
        var versions = evidence.stream()
                .map(Signal::version)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
        return versions.size() == 1 ? Optional.of(versions.getFirst()) : Optional.empty();
    }

    public record Signal(SpringBootBuildEvidenceKind kind, String artifactId,
            Optional<String> version, SourceLocation origin, SourceLocation versionOrigin) {
        public Signal {
            kind = Objects.requireNonNull(kind, "kind");
            artifactId = requireText(artifactId, "artifactId");
            version = Objects.requireNonNull(version, "version");
            if (version.isPresent() && version.orElseThrow().isBlank()) {
                throw new IllegalArgumentException("version must not be blank when present");
            }
            origin = Objects.requireNonNull(origin, "origin");
            versionOrigin = Objects.requireNonNull(versionOrigin, "versionOrigin");
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }
}
