package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Objects;

/** A method parameter with its source spelling. */
public record JavaParameterFact(String name, String type, SourceLocation location) {

    public JavaParameterFact {
        name = requireText(name, "name");
        type = requireText(type, "type");
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
