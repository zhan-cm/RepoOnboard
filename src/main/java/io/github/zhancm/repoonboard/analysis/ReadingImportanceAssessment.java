package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** One source-backed importance signal, before reading-path file deduplication. */
public record ReadingImportanceAssessment(
        String sourceFileId,
        Optional<String> moduleId,
        String subjectId,
        ReadingImportanceRule rule,
        int endpointCount,
        int confirmedIncomingDependencyCount,
        int confirmedOutgoingDependencyCount,
        Optional<Integer> dependencyDistance,
        List<String> supportingDependencyIds) {

    public ReadingImportanceAssessment {
        sourceFileId = SourceLocation.file(sourceFileId).sourceFileId();
        moduleId = requireOptionalText(moduleId, "moduleId");
        subjectId = requireText(subjectId, "subjectId");
        rule = Objects.requireNonNull(rule, "rule");
        requireNonNegative(endpointCount, "endpointCount");
        requireNonNegative(confirmedIncomingDependencyCount, "confirmedIncomingDependencyCount");
        requireNonNegative(confirmedOutgoingDependencyCount, "confirmedOutgoingDependencyCount");
        dependencyDistance = Objects.requireNonNull(dependencyDistance, "dependencyDistance");
        dependencyDistance.ifPresent(distance -> {
            if (distance < 1) {
                throw new IllegalArgumentException("dependencyDistance must be at least 1");
            }
        });
        supportingDependencyIds = Objects.requireNonNull(
                        supportingDependencyIds, "supportingDependencyIds")
                .stream()
                .map(id -> requireText(id, "supportingDependencyId"))
                .distinct()
                .sorted()
                .toList();
        if (rule == ReadingImportanceRule.CONFIRMED_DEPENDENCY_NEIGHBOR) {
            if (dependencyDistance.isEmpty() || supportingDependencyIds.isEmpty()) {
                throw new IllegalArgumentException(
                        "Confirmed dependency neighbors require distance and supporting dependencies");
            }
        } else if (dependencyDistance.isPresent() || !supportingDependencyIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Only confirmed dependency neighbors may carry dependency path support");
        }
    }

    private static Optional<String> requireOptionalText(Optional<String> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(item -> requireText(item, name));
        return value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
