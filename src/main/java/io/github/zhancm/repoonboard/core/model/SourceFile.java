package io.github.zhancm.repoonboard.core.model;

import java.util.Objects;

/** A source file owned by one module. */
public record SourceFile(
        String id, String moduleId, String path, Language language, SourceLocation location) {
    public SourceFile {
        id = ModelValues.requireText(id, "id");
        moduleId = ModelValues.requireText(moduleId, "moduleId");
        path = SourceLocation.file(path).sourceFileId();
        language = Objects.requireNonNull(language, "language");
        location = Objects.requireNonNull(location, "location");
        if (!path.equals(location.sourceFileId())) {
            throw new IllegalArgumentException("path must match location.sourceFileId");
        }
    }
}
