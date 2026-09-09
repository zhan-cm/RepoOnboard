package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Source-backed support for an analysis fact.
 *
 * <p>The type and rule ID are analyzer-neutral, stable identifiers. Analyzer
 * implementations may define their own values without leaking parser or
 * framework objects into the core model.</p>
 */
public record Evidence(
        String type,
        SourceLocation location,
        List<SourceLocation> relatedLocations,
        String ruleId) {

    public Evidence {
        type = requireText(type, "type");
        location = Objects.requireNonNull(location, "location");
        relatedLocations = Objects.requireNonNull(relatedLocations, "relatedLocations").stream()
                .sorted(ModelValues.SOURCE_LOCATION_ORDER)
                .toList();
        ruleId = requireText(ruleId, "ruleId");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
