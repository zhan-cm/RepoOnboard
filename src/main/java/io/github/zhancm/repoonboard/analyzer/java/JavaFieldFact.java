package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;

/** A declared field independent of framework semantics. */
public record JavaFieldFact(
        String name,
        String type,
        List<String> modifiers,
        List<JavaAnnotationFact> annotations,
        SourceLocation location) {

    public JavaFieldFact {
        name = requireText(name, "name");
        type = requireText(type, "type");
        modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
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
