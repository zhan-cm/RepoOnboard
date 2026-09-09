package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A directed component dependency or an explicitly unresolved candidate. */
public record SpringComponentDependencyFact(
        String sourceModulePomFileId,
        String sourceQualifiedName,
        Optional<String> targetModulePomFileId,
        Optional<String> targetQualifiedName,
        Optional<String> resolvedTypeQualifiedName,
        String declaredTargetType,
        SpringDependencyKind kind,
        SpringDependencyStatus status,
        SourceLocation location,
        List<Evidence> evidence) {

    public SpringComponentDependencyFact {
        sourceModulePomFileId = requireText(sourceModulePomFileId, "sourceModulePomFileId");
        sourceQualifiedName = requireText(sourceQualifiedName, "sourceQualifiedName");
        targetModulePomFileId = Objects.requireNonNull(
                targetModulePomFileId, "targetModulePomFileId");
        targetQualifiedName = Objects.requireNonNull(targetQualifiedName, "targetQualifiedName");
        resolvedTypeQualifiedName = Objects.requireNonNull(
                resolvedTypeQualifiedName, "resolvedTypeQualifiedName");
        targetModulePomFileId.ifPresent(value -> requireText(value, "targetModulePomFileId"));
        targetQualifiedName.ifPresent(value -> requireText(value, "targetQualifiedName"));
        resolvedTypeQualifiedName.ifPresent(value -> requireText(value, "resolvedTypeQualifiedName"));
        declaredTargetType = requireText(declaredTargetType, "declaredTargetType");
        kind = Objects.requireNonNull(kind, "kind");
        status = Objects.requireNonNull(status, "status");
        location = Objects.requireNonNull(location, "location");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("evidence must not be empty");
        }
        if (status == SpringDependencyStatus.CONFIRMED
                && (targetModulePomFileId.isEmpty() || targetQualifiedName.isEmpty())) {
            throw new IllegalArgumentException("Confirmed dependency requires a target component");
        }
        if (status == SpringDependencyStatus.CONFIRMED
                && !resolvedTypeQualifiedName.equals(targetQualifiedName)) {
            throw new IllegalArgumentException("Confirmed dependency target must match resolved type");
        }
        if (targetModulePomFileId.isPresent() != targetQualifiedName.isPresent()) {
            throw new IllegalArgumentException("Target module and qualified name must be present together");
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
