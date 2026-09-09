package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Objects;
import java.util.Optional;

/** A type use and its conservative project-local resolution result. */
public record JavaTypeReferenceFact(
        String name,
        JavaTypeReferenceStatus status,
        Optional<String> resolvedQualifiedName,
        SourceLocation location) {

    public JavaTypeReferenceFact {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        status = Objects.requireNonNull(status, "status");
        resolvedQualifiedName = Objects.requireNonNull(resolvedQualifiedName, "resolvedQualifiedName");
        location = Objects.requireNonNull(location, "location");
        if (status == JavaTypeReferenceStatus.RESOLVED && resolvedQualifiedName.isEmpty()) {
            throw new IllegalArgumentException("RESOLVED requires resolvedQualifiedName");
        }
        if (status != JavaTypeReferenceStatus.RESOLVED && resolvedQualifiedName.isPresent()) {
            throw new IllegalArgumentException("Only RESOLVED may have resolvedQualifiedName");
        }
    }

    public static JavaTypeReferenceFact unresolved(String name, SourceLocation location) {
        return new JavaTypeReferenceFact(
                name, JavaTypeReferenceStatus.UNRESOLVED, Optional.empty(), location);
    }

    public JavaTypeReferenceFact resolved(String qualifiedName) {
        return new JavaTypeReferenceFact(
                name, JavaTypeReferenceStatus.RESOLVED, Optional.of(qualifiedName), location);
    }

    public JavaTypeReferenceFact ambiguous() {
        return new JavaTypeReferenceFact(
                name, JavaTypeReferenceStatus.AMBIGUOUS, Optional.empty(), location);
    }
}
