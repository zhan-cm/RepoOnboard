package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;

/** A normalized, evidenced framework version reported for a module. */
public record FrameworkVersion(
        Framework framework,
        String version,
        List<Evidence> evidence) {
    public FrameworkVersion {
        framework = Objects.requireNonNull(framework, "framework");
        version = ModelValues.requireText(version, "version");
        evidence = ModelValues.requireEvidence(evidence);
    }
}
