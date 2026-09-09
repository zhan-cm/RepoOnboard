package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Objects;

/** An existing Java main-source directory owned by one Maven module. */
public record JavaSourceRoot(
        String modulePomFileId,
        String moduleBaseDirectory,
        String relativePath,
        SourceLocation declaration) {

    public JavaSourceRoot {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        moduleBaseDirectory = requireText(moduleBaseDirectory, "moduleBaseDirectory");
        relativePath = ".".equals(relativePath)
                ? relativePath
                : SourceLocation.file(relativePath).sourceFileId();
        declaration = Objects.requireNonNull(declaration, "declaration");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
