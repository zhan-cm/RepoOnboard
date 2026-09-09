package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;

/** A declared method with framework-neutral signature facts. */
public record JavaMethodFact(
        String name,
        String returnType,
        List<JavaParameterFact> parameters,
        List<JavaAnnotationFact> annotations,
        SourceLocation location) {

    public JavaMethodFact {
        name = requireText(name, "name");
        returnType = requireText(returnType, "returnType");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        annotations = List.copyOf(Objects.requireNonNull(annotations, "annotations"));
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
