package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.analyzer.java.JavaCompilationUnitFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeKind;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Confirms direct inheritance from conservative, well-known Spring Data repository bases. */
final class SpringDataRepositoryMatcher {

    private static final Set<String> REPOSITORY_BASES = Set.of(
            "org.springframework.data.repository.Repository",
            "org.springframework.data.repository.CrudRepository",
            "org.springframework.data.repository.ListCrudRepository",
            "org.springframework.data.repository.PagingAndSortingRepository",
            "org.springframework.data.repository.ListPagingAndSortingRepository",
            "org.springframework.data.repository.reactive.ReactiveCrudRepository",
            "org.springframework.data.repository.reactive.ReactiveSortingRepository",
            "org.springframework.data.repository.history.RevisionRepository",
            "org.springframework.data.jpa.repository.JpaRepository");

    private SpringDataRepositoryMatcher() {
    }

    static Resolution resolve(JavaCompilationUnitFact unit, JavaTypeFact type) {
        if (type.kind() != JavaTypeKind.INTERFACE) {
            return new Resolution(List.of(), false);
        }
        List<Match> matches = new ArrayList<>();
        boolean ambiguous = false;
        for (var superType : type.directSuperTypes()) {
            SpringAnnotationResolution resolution = SpringAnnotationMatcher.resolve(
                    superType.name(), unit, REPOSITORY_BASES);
            if (resolution.status() == SpringAnnotationResolution.Status.CONFIRMED) {
                matches.add(new Match(
                        resolution.qualifiedName().orElseThrow(), superType.location()));
            } else if (resolution.status() == SpringAnnotationResolution.Status.AMBIGUOUS) {
                ambiguous = true;
            }
        }
        return new Resolution(matches, ambiguous);
    }

    record Match(String qualifiedName, SourceLocation location) {
    }

    record Resolution(List<Match> matches, boolean ambiguous) {

        Resolution {
            matches = List.copyOf(matches);
        }
    }
}
