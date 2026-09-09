package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.analyzer.java.JavaAnnotationFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaCompilationUnitFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaImportFact;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Confirms framework annotations from qualified names and unambiguous imports. */
final class SpringAnnotationMatcher {

    private SpringAnnotationMatcher() {
    }

    static SpringAnnotationResolution resolve(
            JavaAnnotationFact annotation,
            JavaCompilationUnitFact unit,
            Set<String> knownQualifiedNames) {
        Objects.requireNonNull(annotation, "annotation");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(knownQualifiedNames, "knownQualifiedNames");

        if (knownQualifiedNames.contains(annotation.name())) {
            return SpringAnnotationResolution.confirmed(annotation.name());
        }
        if (annotation.name().contains(".")) {
            return SpringAnnotationResolution.notConfirmed();
        }

        List<String> explicitImports = unit.imports().stream()
                .filter(importFact -> !importFact.wildcard())
                .map(JavaImportFact::name)
                .filter(name -> simpleName(name).equals(annotation.name()))
                .distinct()
                .toList();
        if (!explicitImports.isEmpty()) {
            if (explicitImports.size() != 1) {
                return SpringAnnotationResolution.ambiguous();
            }
            String importedName = explicitImports.getFirst();
            return knownQualifiedNames.contains(importedName)
                    ? SpringAnnotationResolution.confirmed(importedName)
                    : SpringAnnotationResolution.notConfirmed();
        }

        Set<String> wildcardPackages = unit.imports().stream()
                .filter(JavaImportFact::wildcard)
                .filter(importFact -> !importFact.staticImport())
                .map(JavaImportFact::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> candidates = new LinkedHashSet<>();
        for (String packageName : wildcardPackages) {
            String candidate = packageName + "." + annotation.name();
            if (knownQualifiedNames.contains(candidate)) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            return SpringAnnotationResolution.notConfirmed();
        }
        if (candidates.size() == 1 && wildcardPackages.size() == 1) {
            return SpringAnnotationResolution.confirmed(candidates.iterator().next());
        }
        return SpringAnnotationResolution.ambiguous();
    }

    private static String simpleName(String qualifiedName) {
        int separator = qualifiedName.lastIndexOf('.');
        return separator < 0 ? qualifiedName : qualifiedName.substring(separator + 1);
    }
}
