package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;

/** A source-declared constructor and its parameters. */
public record JavaConstructorFact(
        String name,
        List<JavaParameterFact> parameters,
        List<JavaAnnotationFact> annotations,
        SourceLocation location) {

    public JavaConstructorFact {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        annotations = List.copyOf(Objects.requireNonNull(annotations, "annotations"));
        location = Objects.requireNonNull(location, "location");
    }
}
