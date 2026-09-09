package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

final class ModelValues {
    static final Comparator<SourceLocation> SOURCE_LOCATION_ORDER = Comparator
            .comparing(SourceLocation::sourceFileId)
            .thenComparingInt(location -> location.startLine().orElse(0))
            .thenComparingInt(location -> location.startColumn().orElse(0))
            .thenComparingInt(location -> location.endLine().orElse(0))
            .thenComparingInt(location -> location.endColumn().orElse(0))
            .thenComparing(location -> location.symbol().orElse(""));

    static final Comparator<Evidence> EVIDENCE_ORDER = Comparator
            .comparing(Evidence::type)
            .thenComparing(Evidence::location, SOURCE_LOCATION_ORDER)
            .thenComparing(Evidence::ruleId)
            .thenComparing(Evidence::toString);

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
        List<Evidence> copy = stableEvidence(evidence);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("evidence must not be empty");
        }
        return copy;
    }

    static List<Evidence> stableEvidence(List<Evidence> evidence) {
        return Objects.requireNonNull(evidence, "evidence").stream()
                .sorted(EVIDENCE_ORDER)
                .toList();
    }
}
