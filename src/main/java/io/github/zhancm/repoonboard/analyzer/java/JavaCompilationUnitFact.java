package io.github.zhancm.repoonboard.analyzer.java;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Project-owned facts extracted from one compilation unit. */
public record JavaCompilationUnitFact(
        JavaSourceFile sourceFile,
        Optional<String> packageName,
        List<JavaTypeFact> types) {

    public JavaCompilationUnitFact {
        sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
        packageName = Objects.requireNonNull(packageName, "packageName");
        if (packageName.isPresent() && packageName.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("packageName must not be blank when present");
        }
        types = List.copyOf(Objects.requireNonNull(types, "types"));
    }
}
