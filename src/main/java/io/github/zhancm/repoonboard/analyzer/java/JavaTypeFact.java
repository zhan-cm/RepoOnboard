package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A project-local Java type declaration. Nested types use source qualified names. */
public record JavaTypeFact(
        String simpleName,
        String qualifiedName,
        Optional<String> enclosingType,
        JavaTypeKind kind,
        List<JavaAnnotationFact> annotations,
        List<JavaFieldFact> fields,
        List<JavaConstructorFact> constructors,
        List<JavaMethodFact> methods,
        SourceLocation location) {

    public JavaTypeFact {
        simpleName = requireText(simpleName, "simpleName");
        qualifiedName = requireText(qualifiedName, "qualifiedName");
        enclosingType = Objects.requireNonNull(enclosingType, "enclosingType");
        if (enclosingType.isPresent() && enclosingType.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("enclosingType must not be blank when present");
        }
        kind = Objects.requireNonNull(kind, "kind");
        annotations = List.copyOf(Objects.requireNonNull(annotations, "annotations"));
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
        constructors = List.copyOf(Objects.requireNonNull(constructors, "constructors"));
        methods = List.copyOf(Objects.requireNonNull(methods, "methods"));
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
