package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Objects;
import java.util.Optional;

/** A Maven field with its declared value, usable value, state, and origin. */
public record MavenMetadataValue(
        Optional<String> rawValue,
        Optional<String> resolvedValue,
        MavenResolutionStatus resolutionStatus,
        SourceLocation origin) {

    public MavenMetadataValue {
        rawValue = requireOptionalText(rawValue, "rawValue");
        resolvedValue = requireOptionalText(resolvedValue, "resolvedValue");
        resolutionStatus = Objects.requireNonNull(resolutionStatus, "resolutionStatus");
        origin = Objects.requireNonNull(origin, "origin");

        switch (resolutionStatus) {
            case RESOLVED -> {
                if (resolvedValue.isEmpty()) {
                    throw new IllegalArgumentException("RESOLVED requires resolvedValue");
                }
            }
            case UNRESOLVED -> {
                if (rawValue.isEmpty() || resolvedValue.isPresent()) {
                    throw new IllegalArgumentException(
                            "UNRESOLVED requires rawValue and no resolvedValue");
                }
            }
            case MISSING -> {
                if (rawValue.isPresent() || resolvedValue.isPresent()) {
                    throw new IllegalArgumentException("MISSING requires both values to be empty");
                }
            }
        }
    }

    private static Optional<String> requireOptionalText(Optional<String> value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isPresent() && value.orElseThrow().isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank when present");
        }
        return value;
    }
}
