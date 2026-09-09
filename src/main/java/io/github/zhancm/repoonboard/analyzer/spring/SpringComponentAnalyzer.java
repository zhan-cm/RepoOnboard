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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        for (JavaCompilationUnitFact unit : javaAnalysis.compilationUnits()) {
            for (JavaTypeFact type : unit.types()) {
                analyzeType(unit, type, components, diagnostics);
            }
        }
        components.sort(Comparator.comparing(SpringComponentFact::modulePomFileId)
                .thenComparing(SpringComponentFact::qualifiedName));
        return new SpringComponentAnalysis(components, diagnostics);
    }

    private static void analyzeType(
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            List<SpringComponentFact> components,
            List<Diagnostic> diagnostics) {
        if (type.kind() == JavaTypeKind.ANNOTATION) {
            return;
        }

        Map<SpringComponentKind, List<ResolvedAnnotation>> matches = new LinkedHashMap<>();
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
                        .add(new ResolvedAnnotation(annotation, qualifiedName));
            }
        }
        if (matches.isEmpty()) {
            return;
        }
        if (matches.size() > 1) {
            JavaAnnotationFact annotation = matches.values().iterator().next().getFirst().annotation();
            diagnostics.add(diagnostic(
                    "SPRING_COMPONENT_KIND_AMBIGUOUS",
                    unit,
                    type,
                    annotation,
                    "Multiple direct Spring component roles are declared; no single role was selected."));
            return;
        }

        Map.Entry<SpringComponentKind, List<ResolvedAnnotation>> match = matches.entrySet().iterator().next();
        ResolvedAnnotation primary = match.getValue().getFirst();
        Optional<String> componentName = componentName(type, primary.annotation());
        if (hasUnresolvedExplicitName(primary.annotation())) {
            diagnostics.add(diagnostic(
                    "SPRING_COMPONENT_NAME_UNRESOLVED",
                    unit,
                    type,
                    primary.annotation(),
                    "The explicit component name is not a directly readable string literal."));
        }
        List<Evidence> evidence = match.getValue().stream()
                .map(annotation -> new Evidence(
                        "SPRING_COMPONENT_ANNOTATION",
                        annotation.annotation().location(),
                        List.of(type.location()),
                        "spring.component.direct_annotation:" + annotation.qualifiedName()))
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
        return new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(unit.sourceFile().modulePomFileId()),
                Optional.of(type.location().sourceFileId()),
                Optional.of(annotation.location()),
                message);
    }

    private record ResolvedAnnotation(JavaAnnotationFact annotation, String qualifiedName) {
    }
}
