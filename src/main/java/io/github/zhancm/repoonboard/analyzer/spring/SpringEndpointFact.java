package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A resolved or explicitly unresolved Spring MVC HTTP endpoint. */
public record SpringEndpointFact(
        String modulePomFileId,
        String controllerQualifiedName,
        String handlerMethod,
        SpringHttpMethod httpMethod,
        Optional<String> path,
        boolean unresolvedPath,
        SpringMappingConditions conditions,
        SourceLocation location,
        List<Evidence> evidence) {

    public SpringEndpointFact {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        controllerQualifiedName = requireText(controllerQualifiedName, "controllerQualifiedName");
        handlerMethod = requireText(handlerMethod, "handlerMethod");
        httpMethod = Objects.requireNonNull(httpMethod, "httpMethod");
        path = Objects.requireNonNull(path, "path");
        path.ifPresent(value -> requireText(value, "path"));
        if (unresolvedPath == path.isPresent()) {
            throw new IllegalArgumentException(
                    "Exactly one of resolved path or unresolvedPath must be present");
        }
        conditions = Objects.requireNonNull(conditions, "conditions");
        location = Objects.requireNonNull(location, "location");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("evidence must not be empty");
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
