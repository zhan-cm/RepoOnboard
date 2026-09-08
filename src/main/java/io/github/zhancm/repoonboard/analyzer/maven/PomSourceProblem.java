package io.github.zhancm.repoonboard.analyzer.maven;

import java.util.Objects;

record PomSourceProblem(String code, String sourceFileId, String message) {

    PomSourceProblem {
        code = requireText(code, "code");
        sourceFileId = requireText(sourceFileId, "sourceFileId");
        message = requireText(message, "message");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
