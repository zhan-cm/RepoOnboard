package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A confirmed class- or method-level Spring MVC mapping declaration. */
public record SpringMappingFact(
        String modulePomFileId,
        String controllerQualifiedName,
        Optional<String> handlerMethod,
        SpringMappingLevel level,
        List<SpringHttpMethod> httpMethods,
        boolean unresolvedMethod,
        List<String> paths,
        boolean unresolvedPath,
        SpringMappingConditions conditions,
        SourceLocation location,
        List<Evidence> evidence) {

    public SpringMappingFact {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        controllerQualifiedName = requireText(controllerQualifiedName, "controllerQualifiedName");
        handlerMethod = Objects.requireNonNull(handlerMethod, "handlerMethod");
        handlerMethod.ifPresent(value -> requireText(value, "handlerMethod"));
        level = Objects.requireNonNull(level, "level");
        httpMethods = List.copyOf(Objects.requireNonNull(httpMethods, "httpMethods"));
        if (httpMethods.isEmpty()) {
            throw new IllegalArgumentException("httpMethods must not be empty");
        }
        paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
        if (paths.isEmpty() && !unresolvedPath) {
            throw new IllegalArgumentException("Resolved mapping requires at least one path");
        }
        conditions = Objects.requireNonNull(conditions, "conditions");
        location = Objects.requireNonNull(location, "location");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("evidence must not be empty");
        }
        if (level == SpringMappingLevel.TYPE && handlerMethod.isPresent()) {
            throw new IllegalArgumentException("Type mapping must not declare a handler method");
        }
        if (level == SpringMappingLevel.METHOD && handlerMethod.isEmpty()) {
            throw new IllegalArgumentException("Method mapping requires a handler method");
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
