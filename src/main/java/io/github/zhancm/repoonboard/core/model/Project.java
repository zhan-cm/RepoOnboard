package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;

/** The analyzed repository as represented in the public report. */
public record Project(String id, String name, BuildSystem buildSystem, List<Evidence> evidence) {
    public Project {
        id = ModelValues.requireText(id, "id");
        name = ModelValues.requireText(name, "name");
        buildSystem = Objects.requireNonNull(buildSystem, "buildSystem");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
    }
}
