package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** An HTTP endpoint with traceable handler and mapping evidence. */
public record Endpoint(
        String id,
        String moduleId,
        String componentId,
        String httpMethod,
        Optional<String> path,
        boolean unresolvedPath,
        String handlerMethod,
        EndpointConditions conditions,
        Framework framework,
        SourceLocation location,
        List<Evidence> evidence) {
    public Endpoint {
        id = ModelValues.requireText(id, "id");
        moduleId = ModelValues.requireText(moduleId, "moduleId");
        componentId = ModelValues.requireText(componentId, "componentId");
        httpMethod = ModelValues.requireText(httpMethod, "httpMethod");
        path = ModelValues.requireOptionalText(path, "path");
        if (unresolvedPath == path.isPresent()) {
            throw new IllegalArgumentException("Exactly one of path or unresolvedPath must be set");
        }
        handlerMethod = ModelValues.requireText(handlerMethod, "handlerMethod");
        conditions = Objects.requireNonNull(conditions, "conditions");
        framework = Objects.requireNonNull(framework, "framework");
        location = Objects.requireNonNull(location, "location");
        evidence = ModelValues.requireEvidence(evidence);
    }
}
