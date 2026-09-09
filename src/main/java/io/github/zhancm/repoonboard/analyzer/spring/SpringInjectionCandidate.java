package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A constructor parameter or field that Spring may inject. */
public record SpringInjectionCandidate(
        String modulePomFileId,
        String ownerQualifiedName,
        SpringInjectionKind kind,
        String injectionPointName,
        String declaredType,
        SpringInjectionStatus status,
        Optional<String> targetQualifiedName,
        SourceLocation location,
        List<Evidence> evidence) {

    public SpringInjectionCandidate {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        ownerQualifiedName = requireText(ownerQualifiedName, "ownerQualifiedName");
        kind = Objects.requireNonNull(kind, "kind");
        injectionPointName = requireText(injectionPointName, "injectionPointName");
        declaredType = requireText(declaredType, "declaredType");
        status = Objects.requireNonNull(status, "status");
        targetQualifiedName = Objects.requireNonNull(targetQualifiedName, "targetQualifiedName");
        location = Objects.requireNonNull(location, "location");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("evidence must not be empty");
        }
        if (status == SpringInjectionStatus.CONFIRMED && targetQualifiedName.isEmpty()) {
            throw new IllegalArgumentException("CONFIRMED requires targetQualifiedName");
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
