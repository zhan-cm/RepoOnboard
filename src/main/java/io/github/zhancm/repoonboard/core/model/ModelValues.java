package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class ModelValues {
    private ModelValues() {}

    static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    static Optional<String> requireOptionalText(Optional<String> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(item -> requireText(item, name));
        return value;
    }

    static String requireRelativeDirectory(String value, String name) {
        Objects.requireNonNull(value, name);
        return ".".equals(value) ? value : SourceLocation.file(value).sourceFileId();
    }

    static List<Evidence> requireEvidence(List<Evidence> evidence) {
        List<Evidence> copy = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("evidence must not be empty");
        }
        return copy;
    }
}
