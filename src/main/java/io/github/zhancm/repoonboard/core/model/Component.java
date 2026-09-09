package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A framework component represented without framework implementation types. */
public record Component(
        String id,
        String moduleId,
        String qualifiedName,
        Optional<String> name,
        ComponentKind kind,
        Framework framework,
        SourceLocation location,
        List<Evidence> evidence) {
    public Component {
        id = ModelValues.requireText(id, "id");
        moduleId = ModelValues.requireText(moduleId, "moduleId");
        qualifiedName = ModelValues.requireText(qualifiedName, "qualifiedName");
        name = ModelValues.requireOptionalText(name, "name");
        kind = Objects.requireNonNull(kind, "kind");
        framework = Objects.requireNonNull(framework, "framework");
        location = Objects.requireNonNull(location, "location");
        evidence = ModelValues.requireEvidence(evidence);
    }
}
