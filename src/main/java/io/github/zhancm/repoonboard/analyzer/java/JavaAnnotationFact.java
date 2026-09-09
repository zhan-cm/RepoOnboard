package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Objects;

/** An annotation name and its un-interpreted source expression. */
public record JavaAnnotationFact(String name, String expression, SourceLocation location) {

    public JavaAnnotationFact {
        name = requireText(name, "name");
        expression = requireText(expression, "expression");
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
