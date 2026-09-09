package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.analyzer.java.JavaAnnotationFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaCompilationUnitFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeKind;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceStatus;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Resolves project-local annotation composition without inspecting dependencies. */
final class SpringComposedAnnotationResolver {

    private final Map<String, List<AnnotationDeclaration>> declarations;

    SpringComposedAnnotationResolver(JavaParseAnalysis analysis) {
        Map<String, List<AnnotationDeclaration>> indexed = new HashMap<>();
        for (JavaCompilationUnitFact unit : analysis.compilationUnits()) {
            for (JavaTypeFact type : unit.types()) {
                if (type.kind() == JavaTypeKind.ANNOTATION) {
                    indexed.computeIfAbsent(type.qualifiedName(), ignored -> new ArrayList<>())
                            .add(new AnnotationDeclaration(unit, type));
                }
            }
        }
        this.declarations = Map.copyOf(indexed);
    }

    Resolution resolve(
            JavaCompilationUnitFact usageUnit,
            JavaAnnotationFact usage,
            Set<String> knownQualifiedNames) {
        Optional<String> declarationName = resolvedProjectAnnotation(usageUnit, usage);
        if (declarationName.isEmpty()) {
            return Resolution.none();
        }
        return resolveDeclaration(
                declarationName.orElseThrow(), knownQualifiedNames, new LinkedHashSet<>());
    }

    private Resolution resolveDeclaration(
            String qualifiedName,
            Set<String> knownQualifiedNames,
            Set<String> path) {
        List<AnnotationDeclaration> candidates = declarations.getOrDefault(qualifiedName, List.of());
        if (candidates.size() != 1) {
            return candidates.isEmpty() ? Resolution.none() : Resolution.ambiguousResolution();
        }
        if (!path.add(qualifiedName)) {
            return Resolution.ambiguousResolution();
        }

        AnnotationDeclaration declaration = candidates.getFirst();
        List<Match> matches = new ArrayList<>();
        boolean ambiguous = false;
        for (JavaAnnotationFact metaAnnotation : declaration.type().annotations()) {
            SpringAnnotationResolution direct = SpringAnnotationMatcher.resolve(
                    metaAnnotation, declaration.unit(), knownQualifiedNames);
            if (direct.status() == SpringAnnotationResolution.Status.CONFIRMED) {
                matches.add(new Match(
                        direct.qualifiedName().orElseThrow(), metaAnnotation.location()));
                continue;
            }
            if (direct.status() == SpringAnnotationResolution.Status.AMBIGUOUS) {
                ambiguous = true;
                continue;
            }

            Optional<String> nestedName = resolvedProjectAnnotation(declaration.unit(), metaAnnotation);
            if (nestedName.isPresent()) {
                Resolution nested = resolveDeclaration(
                        nestedName.orElseThrow(), knownQualifiedNames, new LinkedHashSet<>(path));
                matches.addAll(nested.matches());
                ambiguous |= nested.ambiguous();
            }
        }
        return new Resolution(distinct(matches), ambiguous);
    }

    private Optional<String> resolvedProjectAnnotation(
            JavaCompilationUnitFact unit, JavaAnnotationFact annotation) {
        return unit.typeReferences().stream()
                .filter(reference -> sameSourceRange(reference, annotation))
                .filter(reference -> reference.status() == JavaTypeReferenceStatus.RESOLVED)
                .map(JavaTypeReferenceFact::resolvedQualifiedName)
                .flatMap(Optional::stream)
                .filter(declarations::containsKey)
                .findFirst();
    }

    private static boolean sameSourceRange(
            JavaTypeReferenceFact reference, JavaAnnotationFact annotation) {
        SourceLocation left = reference.location();
        SourceLocation right = annotation.location();
        return left.sourceFileId().equals(right.sourceFileId())
                && left.startLine().equals(right.startLine())
                && left.startColumn().equals(right.startColumn())
                && left.endLine().equals(right.endLine())
                && left.endColumn().equals(right.endColumn());
    }

    private static List<Match> distinct(List<Match> matches) {
        Map<String, Match> byQualifiedName = new java.util.LinkedHashMap<>();
        for (Match match : matches) {
            byQualifiedName.putIfAbsent(match.qualifiedName(), match);
        }
        return List.copyOf(byQualifiedName.values());
    }

    record Match(String qualifiedName, SourceLocation metaAnnotationLocation) {
    }

    record Resolution(List<Match> matches, boolean ambiguous) {

        Resolution {
            matches = List.copyOf(matches);
        }

        static Resolution none() {
            return new Resolution(List.of(), false);
        }

        static Resolution ambiguousResolution() {
            return new Resolution(List.of(), true);
        }
    }

    private record AnnotationDeclaration(JavaCompilationUnitFact unit, JavaTypeFact type) {
    }
}
