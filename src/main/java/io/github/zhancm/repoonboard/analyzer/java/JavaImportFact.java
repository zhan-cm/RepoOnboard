package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Objects;

/** A Java import declaration without symbol-solver assumptions. */
public record JavaImportFact(
        String name,
        boolean staticImport,
        boolean wildcard,
        SourceLocation location) {

    public JavaImportFact {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        location = Objects.requireNonNull(location, "location");
    }
}
