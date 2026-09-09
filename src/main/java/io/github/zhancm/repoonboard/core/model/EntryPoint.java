package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;

/** A source-confirmed application entry point. */
public record EntryPoint(
        String id,
        String moduleId,
        String qualifiedName,
        String kind,
        Framework framework,
        SourceLocation location,
        List<Evidence> evidence) {
    public EntryPoint {
        id = ModelValues.requireText(id, "id");
        moduleId = ModelValues.requireText(moduleId, "moduleId");
        qualifiedName = ModelValues.requireText(qualifiedName, "qualifiedName");
        kind = ModelValues.requireText(kind, "kind");
        framework = Objects.requireNonNull(framework, "framework");
        location = Objects.requireNonNull(location, "location");
        evidence = ModelValues.requireEvidence(evidence);
    }
}
