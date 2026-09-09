package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.analyzer.java.JavaAnnotationFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaCompilationUnitFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaConstructorFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaFieldFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaMethodFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaParameterFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Extracts injection candidates without constructing dependency graph edges. */
public final class SpringInjectionAnalyzer {

    private static final String STAGE = "SPRING_INJECTION";
    private static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
    private static final Set<String> AUTOWIRED_ANNOTATION = Set.of(AUTOWIRED);
    private static final Set<String> LOMBOK_CONSTRUCTOR_ANNOTATIONS = Set.of(
            "lombok.RequiredArgsConstructor", "lombok.AllArgsConstructor");

    public SpringInjectionAnalysis analyze(
            JavaParseAnalysis javaAnalysis,
            SpringComponentAnalysis components,
            SpringConfigurationAnalysis configurations) {
        Set<OwnerKey> owners = new LinkedHashSet<>();
        components.components().forEach(component -> owners.add(
                new OwnerKey(component.modulePomFileId(), component.qualifiedName())));
        configurations.configurations().forEach(configuration -> owners.add(
                new OwnerKey(configuration.modulePomFileId(), configuration.qualifiedName())));

        List<SpringInjectionCandidate> candidates = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (JavaCompilationUnitFact unit : javaAnalysis.compilationUnits()) {
            for (JavaTypeFact type : unit.types()) {
                if (owners.contains(new OwnerKey(
                        unit.sourceFile().modulePomFileId(), type.qualifiedName()))) {
                    analyzeType(unit, type, candidates, diagnostics);
                }
            }
        }
        candidates.sort(Comparator.comparing(SpringInjectionCandidate::modulePomFileId)
                .thenComparing(SpringInjectionCandidate::ownerQualifiedName)
                .thenComparingInt(candidate -> candidate.location().startLine().orElse(Integer.MAX_VALUE))
                .thenComparing(SpringInjectionCandidate::injectionPointName));
        return new SpringInjectionAnalysis(candidates, diagnostics);
    }

    private static void analyzeType(
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            List<SpringInjectionCandidate> candidates,
            List<Diagnostic> diagnostics) {
        analyzeConstructors(unit, type, candidates, diagnostics);
        analyzeFields(unit, type, candidates, diagnostics);
        auditUnsupportedMethods(unit, type, diagnostics);
        auditLombokConstructor(unit, type, diagnostics);
    }

    private static void analyzeConstructors(
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            List<SpringInjectionCandidate> candidates,
            List<Diagnostic> diagnostics) {
        if (type.constructors().isEmpty()) {
            return;
        }
        if (type.constructors().size() == 1) {
            addConstructorCandidates(
                    unit,
                    type,
                    type.constructors().getFirst(),
                    SpringInjectionStatus.CONFIRMED,
                    "spring.injection.single_constructor",
                    candidates,
                    diagnostics);
            return;
        }

        List<JavaConstructorFact> autowired = new ArrayList<>();
        for (JavaConstructorFact constructor : type.constructors()) {
            AnnotationSearch search = findAnnotation(unit, constructor.annotations(), AUTOWIRED_ANNOTATION);
            if (search.ambiguous()) {
                diagnostics.add(diagnostic(
                        "SPRING_AUTOWIRED_ANNOTATION_AMBIGUOUS",
                        unit,
                        constructor.location(),
                        "An @Autowired constructor could not be confirmed because its imports are ambiguous."));
            }
            if (search.annotation().isPresent()) {
                autowired.add(constructor);
            }
        }
        if (autowired.size() == 1) {
            addConstructorCandidates(
                    unit,
                    type,
                    autowired.getFirst(),
                    SpringInjectionStatus.CONFIRMED,
                    "spring.injection.autowired_constructor",
                    candidates,
                    diagnostics);
            return;
        }

        diagnostics.add(diagnostic(
                "SPRING_CONSTRUCTOR_INJECTION_AMBIGUOUS",
                unit,
                type.location(),
                "Multiple constructors exist without one uniquely confirmed @Autowired constructor."));
        for (JavaConstructorFact constructor : type.constructors()) {
            addConstructorCandidates(
                    unit,
                    type,
                    constructor,
                    SpringInjectionStatus.AMBIGUOUS_CONSTRUCTOR,
                    "spring.injection.ambiguous_constructor_candidate",
                    candidates,
                    diagnostics);
        }
    }

    private static void addConstructorCandidates(
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            JavaConstructorFact constructor,
            SpringInjectionStatus selectionStatus,
            String ruleId,
            List<SpringInjectionCandidate> candidates,
            List<Diagnostic> diagnostics) {
        AnnotationSearch autowired = findAnnotation(unit, constructor.annotations(), AUTOWIRED_ANNOTATION);
        for (JavaParameterFact parameter : constructor.parameters()) {
            List<SourceLocation> related = new ArrayList<>();
            related.add(constructor.location());
            autowired.annotation().map(JavaAnnotationFact::location).ifPresent(related::add);
            candidates.add(candidate(
                    unit,
                    type,
                    SpringInjectionKind.CONSTRUCTOR,
                    parameter.name(),
                    parameter.type(),
                    parameter.location(),
                    selectionStatus,
                    new Evidence("CONSTRUCTOR_INJECTION", parameter.location(), related, ruleId),
                    diagnostics));
        }
    }

    private static void analyzeFields(
            JavaCompilationUnitFact unit,
            JavaTypeFact type,
            List<SpringInjectionCandidate> candidates,
            List<Diagnostic> diagnostics) {
        for (JavaFieldFact field : type.fields()) {
            AnnotationSearch search = findAnnotation(unit, field.annotations(), AUTOWIRED_ANNOTATION);
            if (search.ambiguous()) {
                diagnostics.add(diagnostic(
                        "SPRING_AUTOWIRED_ANNOTATION_AMBIGUOUS",
                        unit,
                        field.location(),
                        "A field @Autowired annotation could not be confirmed because its imports are ambiguous."));
                continue;
            }
            if (search.annotation().isEmpty()) {
                continue;
            }
            if (field.modifiers().contains("static")) {
                diagnostics.add(diagnostic(
                        "SPRING_STATIC_FIELD_INJECTION_IGNORED",
                        unit,
                        field.location(),
                        "Static @Autowired fields are not treated as injection candidates."));
                continue;
            }
            JavaAnnotationFact annotation = search.annotation().orElseThrow();
            candidates.add(candidate(
                    unit,
                    type,
                    SpringInjectionKind.FIELD,
                    field.name(),
                    field.type(),
                    field.location(),
                    SpringInjectionStatus.CONFIRMED,
                    new Evidence(
                            "FIELD_INJECTION",
                            field.location(),
                            List.of(annotation.location()),
                            "spring.injection.autowired_field"),
                    diagnostics));
        }
    }

    private static void auditUnsupportedMethods(
            JavaCompilationUnitFact unit, JavaTypeFact type, List<Diagnostic> diagnostics) {
        for (JavaMethodFact method : type.methods()) {
            AnnotationSearch search = findAnnotation(unit, method.annotations(), AUTOWIRED_ANNOTATION);
            if (search.annotation().isPresent()) {
                diagnostics.add(diagnostic(
                        "SPRING_METHOD_INJECTION_UNSUPPORTED",
                        unit,
                        method.location(),
                        "@Autowired method/setter injection is not analyzed in V0.1."));
            } else if (search.ambiguous()) {
                diagnostics.add(diagnostic(
                        "SPRING_AUTOWIRED_ANNOTATION_AMBIGUOUS",
                        unit,
                        method.location(),
                        "A method @Autowired annotation could not be confirmed because its imports are ambiguous."));
            }
        }
    }

    private static void auditLombokConstructor(
            JavaCompilationUnitFact unit, JavaTypeFact type, List<Diagnostic> diagnostics) {
        if (!type.constructors().isEmpty()) {
            return;
        }
        AnnotationSearch search = findAnnotation(unit, type.annotations(), LOMBOK_CONSTRUCTOR_ANNOTATIONS);
        if (search.annotation().isPresent()) {
            diagnostics.add(diagnostic(
                    "SPRING_LOMBOK_CONSTRUCTOR_UNSUPPORTED",
                    unit,
                    search.annotation().orElseThrow().location(),
                    "Lombok-generated constructors are not inferred from source-only Java facts."));
        }
    }

    private static SpringInjectionCandidate candidate(
            JavaCompilationUnitFact unit,
            JavaTypeFact owner,
            SpringInjectionKind kind,
            String pointName,
            String declaredType,
            SourceLocation location,
            SpringInjectionStatus requestedStatus,
            Evidence evidence,
            List<Diagnostic> diagnostics) {
        Optional<JavaTypeReferenceFact> reference = typeReference(unit, location, declaredType);
        Optional<String> target = reference
                .filter(candidate -> candidate.status() == JavaTypeReferenceStatus.RESOLVED)
                .flatMap(JavaTypeReferenceFact::resolvedQualifiedName);
        SpringInjectionStatus status = requestedStatus;
        if (requestedStatus == SpringInjectionStatus.CONFIRMED && target.isEmpty()) {
            status = SpringInjectionStatus.UNRESOLVED_TYPE;
        }
        if (declaredType.contains("<")) {
            diagnostics.add(diagnostic(
                    "SPRING_GENERIC_INJECTION_UNSUPPORTED",
                    unit,
                    location,
                    "Generic/container injection targets are retained but not resolved as component targets."));
            target = Optional.empty();
            if (requestedStatus == SpringInjectionStatus.CONFIRMED) {
                status = SpringInjectionStatus.UNRESOLVED_TYPE;
            }
        }
        return new SpringInjectionCandidate(
                unit.sourceFile().modulePomFileId(),
                owner.qualifiedName(),
                kind,
                pointName,
                declaredType,
                status,
                target,
                location,
                List.of(evidence));
    }

    private static Optional<JavaTypeReferenceFact> typeReference(
            JavaCompilationUnitFact unit, SourceLocation location, String declaredType) {
        String baseType = declaredType.replace("[]", "").replace("...", "");
        List<JavaTypeReferenceFact> contained = unit.typeReferences().stream()
                .filter(reference -> contains(location, reference.location()))
                .toList();
        List<JavaTypeReferenceFact> exact = contained.stream()
                .filter(reference -> reference.name().equals(baseType))
                .toList();
        if (exact.size() == 1) {
            return Optional.of(exact.getFirst());
        }
        return contained.size() == 1 ? Optional.of(contained.getFirst()) : Optional.empty();
    }

    private static boolean contains(SourceLocation outer, SourceLocation inner) {
        if (!outer.sourceFileId().equals(inner.sourceFileId())
                || outer.startLine().isEmpty()
                || outer.startColumn().isEmpty()
                || outer.endLine().isEmpty()
                || outer.endColumn().isEmpty()
                || inner.startLine().isEmpty()
                || inner.startColumn().isEmpty()
                || inner.endLine().isEmpty()
                || inner.endColumn().isEmpty()) {
            return false;
        }
        return compare(
                        outer.startLine().orElseThrow(),
                        outer.startColumn().orElseThrow(),
                        inner.startLine().orElseThrow(),
                        inner.startColumn().orElseThrow()) <= 0
                && compare(
                        outer.endLine().orElseThrow(),
                        outer.endColumn().orElseThrow(),
                        inner.endLine().orElseThrow(),
                        inner.endColumn().orElseThrow()) >= 0;
    }

    private static int compare(int leftLine, int leftColumn, int rightLine, int rightColumn) {
        int line = Integer.compare(leftLine, rightLine);
        return line != 0 ? line : Integer.compare(leftColumn, rightColumn);
    }

    private static AnnotationSearch findAnnotation(
            JavaCompilationUnitFact unit,
            List<JavaAnnotationFact> annotations,
            Set<String> knownNames) {
        List<JavaAnnotationFact> confirmed = new ArrayList<>();
        boolean ambiguous = false;
        for (JavaAnnotationFact annotation : annotations) {
            SpringAnnotationResolution resolution = SpringAnnotationMatcher.resolve(
                    annotation, unit, knownNames);
            if (resolution.status() == SpringAnnotationResolution.Status.CONFIRMED) {
                confirmed.add(annotation);
            } else if (resolution.status() == SpringAnnotationResolution.Status.AMBIGUOUS) {
                ambiguous = true;
            }
        }
        return new AnnotationSearch(
                confirmed.size() == 1 ? Optional.of(confirmed.getFirst()) : Optional.empty(),
                ambiguous || confirmed.size() > 1);
    }

    private static Diagnostic diagnostic(
            String code,
            JavaCompilationUnitFact unit,
            SourceLocation location,
            String message) {
        return new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(unit.sourceFile().modulePomFileId()),
                Optional.of(location.sourceFileId()),
                Optional.of(location),
                message);
    }

    private record OwnerKey(String modulePomFileId, String qualifiedName) {
    }

    private record AnnotationSearch(Optional<JavaAnnotationFact> annotation, boolean ambiguous) {
    }
}
