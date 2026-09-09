package io.github.zhancm.repoonboard.analyzer.spring;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Detects confirmed Spring configuration classes and Boot application entry points. */
public final class SpringConfigurationAnalyzer {

    private static final String STAGE = "SPRING_CONFIGURATION";
    private static final String CONFIGURATION = "org.springframework.context.annotation.Configuration";
    private static final String SPRING_BOOT_APPLICATION =
            "org.springframework.boot.autoconfigure.SpringBootApplication";
    private static final Set<String> KNOWN_ANNOTATIONS = Set.of(CONFIGURATION, SPRING_BOOT_APPLICATION);

    public SpringConfigurationAnalysis analyze(JavaParseAnalysis javaAnalysis) {
        List<SpringConfigurationFact> configurations = new ArrayList<>();
        List<SpringEntryPointFact> entryPoints = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        SpringComposedAnnotationResolver composedResolver =
                new SpringComposedAnnotationResolver(javaAnalysis);
        for (JavaCompilationUnitFact unit : javaAnalysis.compilationUnits()) {
            for (JavaTypeFact type : unit.types()) {
                analyzeType(unit, type, composedResolver, configurations, entryPoints, diagnostics);
            }
        }
        configurations.sort(Comparator.comparing(SpringConfigurationFact::modulePomFileId)
                .thenComparing(SpringConfigurationFact::qualifiedName));
        entryPoints.sort(Comparator.comparing(SpringEntryPointFact::modulePomFileId)
                .thenComparing(SpringEntryPointFact::qualifiedName));
        return new SpringConfigurationAnalysis(configurations, entryPoints, diagnostics);
    }

    private static void analyzeType(
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            SpringComposedAnnotationResolver composedResolver,
            List<SpringConfigurationFact> configurations,
            List<SpringEntryPointFact> entryPoints,
            List<Diagnostic> diagnostics) {
        if (type.kind() == JavaTypeKind.ANNOTATION) {
            return;
        }
        List<ResolvedAnnotation> matches = new ArrayList<>();
        boolean ambiguousComposition = false;
        for (JavaAnnotationFact annotation : type.annotations()) {
            SpringAnnotationResolution resolution = SpringAnnotationMatcher.resolve(
                    annotation, unit, KNOWN_ANNOTATIONS);
            if (resolution.status() == SpringAnnotationResolution.Status.AMBIGUOUS) {
                diagnostics.add(diagnostic(unit, type, annotation));
            } else if (resolution.status() == SpringAnnotationResolution.Status.CONFIRMED) {
                matches.add(new ResolvedAnnotation(
                        annotation, resolution.qualifiedName().orElseThrow(), Optional.empty()));
            } else {
                SpringComposedAnnotationResolver.Resolution composed = composedResolver.resolve(
                        unit, annotation, KNOWN_ANNOTATIONS);
                if (composed.ambiguous()) {
                    ambiguousComposition = true;
                    diagnostics.add(composedDiagnostic(unit, type, annotation));
                }
                for (SpringComposedAnnotationResolver.Match composedMatch : composed.matches()) {
                    matches.add(new ResolvedAnnotation(
                            annotation,
                            composedMatch.qualifiedName(),
                            Optional.of(composedMatch.metaAnnotationLocation())));
                }
            }
        }
        if (ambiguousComposition) {
            return;
        }
        if (matches.isEmpty()) {
            return;
        }

        List<Evidence> configurationEvidence = matches.stream()
                .map(match -> evidence(
                        "SPRING_CONFIGURATION_ANNOTATION",
                        match,
                        type,
                        "spring.configuration.annotation:" + match.qualifiedName()))
                .toList();
        configurations.add(new SpringConfigurationFact(
                unit.sourceFile().modulePomFileId(),
                type.qualifiedName(),
                type.location(),
                configurationEvidence));

        List<Evidence> entryEvidence = matches.stream()
                .filter(match -> match.qualifiedName().equals(SPRING_BOOT_APPLICATION))
                .map(match -> evidence(
                        "SPRING_BOOT_APPLICATION_ANNOTATION",
                        match,
                        type,
                        "spring.entry_point.spring_boot_application"))
                .toList();
        if (!entryEvidence.isEmpty()) {
            entryPoints.add(new SpringEntryPointFact(
                    unit.sourceFile().modulePomFileId(),
                    type.qualifiedName(),
                    SpringEntryPointKind.APPLICATION,
                    type.location(),
                    entryEvidence));
        }
    }

    private static Evidence evidence(
            String type,
            ResolvedAnnotation annotation,
            JavaTypeFact declaration,
            String ruleId) {
        return new Evidence(
                annotation.metaAnnotationLocation().isPresent()
                        ? "SPRING_COMPOSED_" + type.substring("SPRING_".length())
                        : type,
                annotation.annotation().location(),
                annotation.metaAnnotationLocation().isPresent()
                        ? List.of(declaration.location(), annotation.metaAnnotationLocation().orElseThrow())
                        : List.of(declaration.location()),
                annotation.metaAnnotationLocation().isPresent()
                        ? ruleId.replaceFirst("spring\\.", "spring.composed.")
                        : ruleId);
    }

    private static Diagnostic diagnostic(
            JavaCompilationUnitFact unit, JavaTypeFact type, JavaAnnotationFact annotation) {
        return new Diagnostic(
                "SPRING_CONFIGURATION_ANNOTATION_AMBIGUOUS",
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(unit.sourceFile().modulePomFileId()),
                Optional.of(type.location().sourceFileId()),
                Optional.of(annotation.location()),
                "A Spring configuration annotation could not be confirmed because its imports are ambiguous.");
    }

    private static Diagnostic composedDiagnostic(
            JavaCompilationUnitFact unit, JavaTypeFact type, JavaAnnotationFact annotation) {
        return new Diagnostic(
                "SPRING_COMPOSED_CONFIGURATION_AMBIGUOUS",
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(unit.sourceFile().modulePomFileId()),
                Optional.of(type.location().sourceFileId()),
                Optional.of(annotation.location()),
                "A project-local composed configuration annotation is cyclic or cannot be uniquely resolved.");
    }

    private record ResolvedAnnotation(
            JavaAnnotationFact annotation,
            String qualifiedName,
            Optional<io.github.zhancm.repoonboard.core.model.SourceLocation> metaAnnotationLocation) {
    }
}
