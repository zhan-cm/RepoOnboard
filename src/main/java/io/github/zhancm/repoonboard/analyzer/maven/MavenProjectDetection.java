package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.Evidence;
import java.util.Objects;
import java.util.Optional;

/** The explicit result of checking a scan root for a Maven project. */
public record MavenProjectDetection(boolean detected, Optional<Evidence> evidence) {

    public MavenProjectDetection {
        evidence = Objects.requireNonNull(evidence, "evidence");
        if (detected != evidence.isPresent()) {
            throw new IllegalArgumentException(
                    "A detected Maven project must have evidence, and a non-Maven result must not");
        }
    }

    static MavenProjectDetection detected(Evidence evidence) {
        return new MavenProjectDetection(true, Optional.of(evidence));
    }

    static MavenProjectDetection notDetected() {
        return new MavenProjectDetection(false, Optional.empty());
    }
}
