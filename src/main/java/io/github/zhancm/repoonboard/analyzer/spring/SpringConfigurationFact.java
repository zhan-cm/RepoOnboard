package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;

/** A source-confirmed Spring configuration type. */
public record SpringConfigurationFact(
        String modulePomFileId,
        String qualifiedName,
        SourceLocation location,
        List<Evidence> evidence) {

    public SpringConfigurationFact {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        qualifiedName = requireText(qualifiedName, "qualifiedName");
        location = Objects.requireNonNull(location, "location");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("evidence must not be empty");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
