package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.core.model.Evidence;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A user-readable recommendation reason backed by exact report entities and evidence. */
public record RecommendationReason(
        RecommendationReasonKind kind,
        String message,
        int factCount,
        Optional<Integer> dependencyDistance,
        List<String> supportingEntityIds,
        List<Evidence> evidence) {

    private static final Comparator<Evidence> EVIDENCE_ORDER = Comparator
            .comparing(Evidence::type)
            .thenComparing(value -> value.location().toString())
            .thenComparing(Evidence::ruleId)
            .thenComparing(Evidence::toString);

    public RecommendationReason {
        kind = Objects.requireNonNull(kind, "kind");
        message = requireText(message, "message");
        if (factCount < 1) {
            throw new IllegalArgumentException("factCount must be at least 1");
        }
        dependencyDistance = Objects.requireNonNull(dependencyDistance, "dependencyDistance");
        dependencyDistance.ifPresent(distance -> {
            if (distance < 1) {
                throw new IllegalArgumentException("dependencyDistance must be at least 1");
            }
        });
        supportingEntityIds = Objects.requireNonNull(
                        supportingEntityIds, "supportingEntityIds")
                .stream()
                .map(id -> requireText(id, "supportingEntityId"))
                .distinct()
                .sorted()
                .toList();
        if (supportingEntityIds.isEmpty()) {
            throw new IllegalArgumentException("supportingEntityIds must not be empty");
        }
        evidence = Objects.requireNonNull(evidence, "evidence").stream()
                .distinct()
                .sorted(EVIDENCE_ORDER)
                .toList();
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("evidence must not be empty");
        }
        boolean dependencyPath = kind == RecommendationReasonKind.CONFIRMED_DEPENDENCY_PATH;
        if (dependencyPath != dependencyDistance.isPresent()) {
            throw new IllegalArgumentException(
                    "Only confirmed dependency paths carry dependencyDistance");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
