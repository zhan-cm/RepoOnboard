package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;

/** One annotation attribute with raw expression and confirmed string literals. */
public record JavaAnnotationAttributeFact(
        String name,
        String expression,
        List<String> stringLiterals,
        SourceLocation location) {

    public JavaAnnotationAttributeFact {
        name = requireText(name, "name");
        expression = requireText(expression, "expression");
        stringLiterals = List.copyOf(Objects.requireNonNull(stringLiterals, "stringLiterals"));
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
