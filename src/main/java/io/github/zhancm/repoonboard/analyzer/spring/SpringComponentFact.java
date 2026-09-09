package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A source-confirmed Spring component role. */
public record SpringComponentFact(
        String modulePomFileId,
        String qualifiedName,
        Optional<String> name,
        SpringComponentKind kind,
        SourceLocation location,
        List<Evidence> evidence) {

    public SpringComponentFact {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        qualifiedName = requireText(qualifiedName, "qualifiedName");
        name = Objects.requireNonNull(name, "name");
        if (name.isPresent() && name.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("name must not be blank when present");
        }
        kind = Objects.requireNonNull(kind, "kind");
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
