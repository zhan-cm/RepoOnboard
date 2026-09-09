package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Objects;
import java.util.Optional;

/** A module-owned Java type declaration stored in the declaration index. */
public record JavaDeclarationEntry(
        String modulePomFileId,
        Optional<String> packageName,
        String qualifiedName,
        String simpleName,
        JavaTypeKind kind,
        SourceLocation location) {

    public JavaDeclarationEntry {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        packageName = Objects.requireNonNull(packageName, "packageName");
        if (packageName.isPresent() && packageName.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("packageName must not be blank when present");
        }
        qualifiedName = requireText(qualifiedName, "qualifiedName");
        simpleName = requireText(simpleName, "simpleName");
        kind = Objects.requireNonNull(kind, "kind");
        location = Objects.requireNonNull(location, "location");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
