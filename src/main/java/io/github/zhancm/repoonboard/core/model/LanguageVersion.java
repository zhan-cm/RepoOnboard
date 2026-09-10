package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;

/** A normalized, evidenced language version reported for a module. */
public record LanguageVersion(
        Language language,
        String version,
        List<Evidence> evidence) {
    public LanguageVersion {
        language = Objects.requireNonNull(language, "language");
        version = ModelValues.requireText(version, "version");
        evidence = ModelValues.requireEvidence(evidence);
    }
}
