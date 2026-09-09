package io.github.zhancm.repoonboard.analyzer.spring;

import java.util.Objects;
import java.util.Optional;

record SpringAnnotationResolution(
        Status status,
        Optional<String> qualifiedName) {

    SpringAnnotationResolution {
        status = Objects.requireNonNull(status, "status");
        qualifiedName = Objects.requireNonNull(qualifiedName, "qualifiedName");
        if (status == Status.CONFIRMED && qualifiedName.isEmpty()) {
            throw new IllegalArgumentException("CONFIRMED requires qualifiedName");
        }
        if (status != Status.CONFIRMED && qualifiedName.isPresent()) {
            throw new IllegalArgumentException("Only CONFIRMED may contain qualifiedName");
        }
    }

    static SpringAnnotationResolution confirmed(String qualifiedName) {
        return new SpringAnnotationResolution(Status.CONFIRMED, Optional.of(qualifiedName));
    }

    static SpringAnnotationResolution notConfirmed() {
        return new SpringAnnotationResolution(Status.NOT_CONFIRMED, Optional.empty());
    }

    static SpringAnnotationResolution ambiguous() {
        return new SpringAnnotationResolution(Status.AMBIGUOUS, Optional.empty());
    }

    enum Status {
        CONFIRMED,
        NOT_CONFIRMED,
        AMBIGUOUS
    }
}
