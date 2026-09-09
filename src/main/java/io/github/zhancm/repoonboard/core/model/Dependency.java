package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A typed directed edge, including explicitly unresolved targets. */
public record Dependency(
        String id,
        String sourceId,
        Optional<String> targetId,
        String declaredTarget,
        DependencyKind kind,
        ResolutionStatus status,
        SourceLocation location,
        List<Evidence> evidence) {
    public Dependency {
        id = ModelValues.requireText(id, "id");
        sourceId = ModelValues.requireText(sourceId, "sourceId");
        targetId = ModelValues.requireOptionalText(targetId, "targetId");
        declaredTarget = ModelValues.requireText(declaredTarget, "declaredTarget");
        kind = Objects.requireNonNull(kind, "kind");
        status = Objects.requireNonNull(status, "status");
        location = Objects.requireNonNull(location, "location");
        evidence = ModelValues.requireEvidence(evidence);
        if (status == ResolutionStatus.CONFIRMED && targetId.isEmpty()) {
            throw new IllegalArgumentException("Confirmed dependency requires targetId");
        }
    }
}
