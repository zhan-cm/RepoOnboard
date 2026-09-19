package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.analyzer.java.JavaAnnotationAttributeFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaAnnotationFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaCompilationUnitFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeKind;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Maps confirmed standard Spring stereotype annotations to component facts. */
public final class SpringComponentAnalyzer {

    private static final String STAGE = "SPRING_COMPONENT";
    private static final Map<String, SpringComponentKind> COMPONENT_ANNOTATIONS = Map.of(
            "org.springframework.stereotype.Controller", SpringComponentKind.CONTROLLER,
            "org.springframework.web.bind.annotation.RestController", SpringComponentKind.REST_CONTROLLER,
            "org.springframework.stereotype.Service", SpringComponentKind.SERVICE,
            "org.springframework.stereotype.Repository", SpringComponentKind.REPOSITORY,
            "org.springframework.stereotype.Component", SpringComponentKind.COMPONENT);

    public SpringComponentAnalysis analyze(JavaParseAnalysis javaAnalysis) {
        List<SpringComponentFact> components = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        SpringComposedAnnotationResolver composedResolver =
                new SpringComposedAnnotationResolver(javaAnalysis);
        for (JavaCompilationUnitFact unit : javaAnalysis.compilationUnits()) {
            for (JavaTypeFact type : unit.types()) {
                analyzeType(unit, type, composedResolver, components, diagnostics);
            }
        }
        components.sort(Comparator.comparing(SpringComponentFact::modulePomFileId)
                .thenComparing(SpringComponentFact::qualifiedName));
        return new SpringComponentAnalysis(components, diagnostics);
    }

    private static void analyzeType(
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            SpringComposedAnnotationResolver composedResolver,
            List<SpringComponentFact> components,
            List<Diagnostic> diagnostics) {
        if (type.kind() == JavaTypeKind.ANNOTATION) {
            return;
        }

        Map<SpringComponentKind, List<ResolvedComponentMatch>> matches = new LinkedHashMap<>();
        boolean ambiguousComposition = false;
        for (JavaAnnotationFact annotation : type.annotations()) {
            SpringAnnotationResolution resolution = SpringAnnotationMatcher.resolve(
                    annotation, unit, COMPONENT_ANNOTATIONS.keySet());
            if (resolution.status() == SpringAnnotationResolution.Status.AMBIGUOUS) {
                diagnostics.add(diagnostic(
                        "SPRING_COMPONENT_ANNOTATION_AMBIGUOUS",
                        unit,
                        type,
                        annotation,
                        "A component annotation could not be confirmed because its imports are ambiguous."));
            } else if (resolution.status() == SpringAnnotationResolution.Status.CONFIRMED) {
                String qualifiedName = resolution.qualifiedName().orElseThrow();
                SpringComponentKind kind = COMPONENT_ANNOTATIONS.get(qualifiedName);
                matches.computeIfAbsent(kind, ignored -> new ArrayList<>())
                        .add(ResolvedComponentMatch.direct(annotation, qualifiedName, type.location()));
            } else {
                SpringComposedAnnotationResolver.Resolution composed = composedResolver.resolve(
                        unit, annotation, COMPONENT_ANNOTATIONS.keySet());
                if (composed.ambiguous()) {
                    ambiguousComposition = true;
                    diagnostics.add(diagnostic(
                            "SPRING_COMPOSED_COMPONENT_AMBIGUOUS",
                            unit,
                            type,
                            annotation,
                            "A project-local composed component annotation is cyclic or cannot be uniquely resolved."));
                }
                for (SpringComposedAnnotationResolver.Match composedMatch : composed.matches()) {
                    SpringComponentKind kind = COMPONENT_ANNOTATIONS.get(composedMatch.qualifiedName());
                    matches.computeIfAbsent(kind, ignored -> new ArrayList<>())
                            .add(ResolvedComponentMatch.composed(annotation, composedMatch, type.location()));
                }
            }
        }
        if (ambiguousComposition) {
            return;
        }

        SpringDataRepositoryMatcher.Resolution repositoryInheritance =
                SpringDataRepositoryMatcher.resolve(unit, type);
        if (repositoryInheritance.ambiguous()) {
            diagnostics.add(diagnostic(
                    "SPRING_DATA_REPOSITORY_INHERITANCE_AMBIGUOUS",
                    unit,
                    type,
                    type.location(),
                    "A Spring Data repository base type could not be confirmed because its imports are ambiguous."));
        }
        for (SpringDataRepositoryMatcher.Match inherited : repositoryInheritance.matches()) {
            matches.computeIfAbsent(SpringComponentKind.REPOSITORY, ignored -> new ArrayList<>())
                    .add(ResolvedComponentMatch.inherited(inherited, type.location()));
        }
        if (matches.isEmpty()) {
            return;
        }
        if (matches.size() > 1) {
            ResolvedComponentMatch first = matches.values().iterator().next().getFirst();
            diagnostics.add(diagnostic(
                    "SPRING_COMPONENT_KIND_AMBIGUOUS",
                    unit,
                    type,
                    first.evidence().location(),
                    "Multiple Spring component roles are declared or composed; no single role was selected."));
            return;
        }

        Map.Entry<SpringComponentKind, List<ResolvedComponentMatch>> match =
                matches.entrySet().iterator().next();
        Optional<JavaAnnotationFact> namingAnnotation = match.getValue().stream()
                .flatMap(item -> item.namingAnnotation().stream())
                .findFirst();
        Optional<String> componentName = namingAnnotation
                .map(annotation -> componentName(type, annotation))
                .orElseGet(() -> Optional.of(defaultComponentName(type.simpleName())));
        if (namingAnnotation.isPresent()
                && hasUnresolvedExplicitName(namingAnnotation.orElseThrow())) {
            diagnostics.add(diagnostic(
                    "SPRING_COMPONENT_NAME_UNRESOLVED",
                    unit,
                    type,
                    namingAnnotation.orElseThrow(),
                    "The explicit component name is not a directly readable string literal."));
        }
        List<Evidence> evidence = match.getValue().stream()
                .map(ResolvedComponentMatch::evidence)
                .toList();
        components.add(new SpringComponentFact(
                unit.sourceFile().modulePomFileId(),
                type.qualifiedName(),
                componentName,
                match.getKey(),
                type.location(),
                evidence));
    }

    private static Optional<String> componentName(JavaTypeFact type, JavaAnnotationFact annotation) {
        Optional<JavaAnnotationAttributeFact> value = annotation.attributes().stream()
                .filter(attribute -> attribute.name().equals("value"))
                .findFirst();
        if (value.isEmpty()) {
            return Optional.of(defaultComponentName(type.simpleName()));
        }
        List<String> literals = value.orElseThrow().stringLiterals();
        if (literals.size() == 1) {
            String literal = literals.getFirst();
            return literal.isBlank()
                    ? Optional.of(defaultComponentName(type.simpleName()))
                    : Optional.of(literal);
        }
        return Optional.empty();
    }

    private static boolean hasUnresolvedExplicitName(JavaAnnotationFact annotation) {
        return annotation.attributes().stream()
                .filter(attribute -> attribute.name().equals("value"))
                .anyMatch(attribute -> attribute.stringLiterals().size() != 1);
    }

    private static String defaultComponentName(String simpleName) {
        if (simpleName.length() > 1
                && Character.isUpperCase(simpleName.charAt(0))
                && Character.isUpperCase(simpleName.charAt(1))) {
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private static Diagnostic diagnostic(
            String code,
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            JavaAnnotationFact annotation,
            String message) {
        return diagnostic(code, unit, type, annotation.location(), message);
    }

    private static Diagnostic diagnostic(
            String code,
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            SourceLocation location,
            String message) {
        return new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(unit.sourceFile().modulePomFileId()),
                Optional.of(type.location().sourceFileId()),
                Optional.of(location),
                message);
    }

    private record ResolvedComponentMatch(
            Optional<JavaAnnotationFact> namingAnnotation,
            Evidence evidence) {

        static ResolvedComponentMatch direct(
                JavaAnnotationFact annotation, String qualifiedName, SourceLocation typeLocation) {
            return new ResolvedComponentMatch(
                    Optional.of(annotation),
                    new Evidence(
                            "SPRING_COMPONENT_ANNOTATION",
                            annotation.location(),
                            List.of(typeLocation),
                            "spring.component.direct_annotation:" + qualifiedName));
        }

        static ResolvedComponentMatch composed(
                JavaAnnotationFact annotation,
                SpringComposedAnnotationResolver.Match match,
                SourceLocation typeLocation) {
            return new ResolvedComponentMatch(
                    Optional.empty(),
                    new Evidence(
                            "SPRING_COMPOSED_COMPONENT_ANNOTATION",
                            annotation.location(),
                            List.of(typeLocation, match.metaAnnotationLocation()),
                            "spring.component.composed_annotation:" + match.qualifiedName()));
        }

        static ResolvedComponentMatch inherited(
                SpringDataRepositoryMatcher.Match match, SourceLocation typeLocation) {
            return new ResolvedComponentMatch(
                    Optional.empty(),
                    new Evidence(
                            "SPRING_DATA_REPOSITORY_INHERITANCE",
                            match.location(),
                            List.of(typeLocation),
                            "spring.component.repository_inheritance:" + match.qualifiedName()));
        }
    }
}
