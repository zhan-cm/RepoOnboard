package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Objects;

/** A Java file with stable scan-root-relative identity and Maven module ownership. */
public record JavaSourceFile(
        String modulePomFileId,
        String moduleBaseDirectory,
        String sourceRootRelativePath,
        String relativePath,
        SourceLocation location) {

    public JavaSourceFile {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        moduleBaseDirectory = requireText(moduleBaseDirectory, "moduleBaseDirectory");
        sourceRootRelativePath = requireRelativePath(
                sourceRootRelativePath, "sourceRootRelativePath");
        relativePath = SourceLocation.file(relativePath).sourceFileId();
        location = Objects.requireNonNull(location, "location");
        if (!relativePath.equals(location.sourceFileId())) {
            throw new IllegalArgumentException("relativePath must match location.sourceFileId");
        }
    }

    private static String requireRelativePath(String value, String name) {
        Objects.requireNonNull(value, name);
        return ".".equals(value) ? value : SourceLocation.file(value).sourceFileId();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
