package io.github.zhancm.repoonboard.core.model;

import java.util.Objects;
import java.util.Optional;

/** A structured, locatable explanation of an analysis issue or event. */
public record Diagnostic(
        String code,
        DiagnosticSeverity severity,
        String stage,
        Optional<String> moduleId,
        Optional<String> fileId,
        Optional<SourceLocation> location,
        String message) {

    public Diagnostic {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        stage = requireText(stage, "stage");
        moduleId = requireOptionalText(moduleId, "moduleId");
        fileId = requireOptionalText(fileId, "fileId")
                .map(SourceLocation::requireSourceFileId);
        location = Objects.requireNonNull(location, "location");
        message = requireText(message, "message");

        if (fileId.isPresent() && location.isPresent()
                && !fileId.orElseThrow().equals(location.orElseThrow().sourceFileId())) {
            throw new IllegalArgumentException("fileId must match location.sourceFileId");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static Optional<String> requireOptionalText(Optional<String> value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isPresent() && value.orElseThrow().isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank when present");
        }
        return value;
    }
}
