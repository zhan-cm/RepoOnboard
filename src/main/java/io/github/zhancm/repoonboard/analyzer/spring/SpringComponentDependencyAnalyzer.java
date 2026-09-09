package io.github.zhancm.repoonboard.analyzer.spring;

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
import java.util.stream.Collectors;

/** Builds consolidated component dependencies from Spring injection candidates. */
public final class SpringComponentDependencyAnalyzer {

    private static final String STAGE = "SPRING_COMPONENT_DEPENDENCY";

    public SpringComponentDependencyAnalysis analyze(
            SpringComponentAnalysis componentAnalysis,
            SpringInjectionAnalysis injectionAnalysis) {
        Map<ComponentKey, SpringComponentFact> components = componentAnalysis.components().stream()
                .collect(Collectors.toMap(
                        component -> new ComponentKey(
                                component.modulePomFileId(), component.qualifiedName()),
                        component -> component,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, List<SpringComponentFact>> componentsByName = componentAnalysis.components().stream()
                .collect(Collectors.groupingBy(
                        SpringComponentFact::qualifiedName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<DependencyKey, DependencyAccumulator> consolidated = new LinkedHashMap<>();
        for (SpringInjectionCandidate candidate : injectionAnalysis.candidates()) {
            if (!components.containsKey(new ComponentKey(
                    candidate.modulePomFileId(), candidate.ownerQualifiedName()))) {
                continue;
            }
            ResolvedTarget target = resolveTarget(candidate, componentsByName);
            DependencyKey key = DependencyKey.from(candidate, target);
            consolidated.computeIfAbsent(key, ignored -> new DependencyAccumulator(candidate, target))
                    .add(candidate);
        }

        List<SpringComponentDependencyFact> dependencies = consolidated.values().stream()
                .map(DependencyAccumulator::toFact)
                .sorted(Comparator.comparing(SpringComponentDependencyFact::sourceModulePomFileId)
                        .thenComparing(SpringComponentDependencyFact::sourceQualifiedName)
                        .thenComparing(dependency -> dependency.targetModulePomFileId().orElse(""))
                        .thenComparing(dependency -> dependency.targetQualifiedName().orElse(""))
                        .thenComparing(SpringComponentDependencyFact::declaredTargetType)
                        .thenComparing(dependency -> dependency.status().name()))
                .toList();
        List<Diagnostic> diagnostics = dependencies.stream()
                .filter(dependency -> dependency.status() != SpringDependencyStatus.CONFIRMED)
                .map(SpringComponentDependencyAnalyzer::diagnostic)
                .toList();
        return new SpringComponentDependencyAnalysis(dependencies, diagnostics);
    }

    private static ResolvedTarget resolveTarget(
            SpringInjectionCandidate candidate,
            Map<String, List<SpringComponentFact>> componentsByName) {
        if (candidate.status() == SpringInjectionStatus.AMBIGUOUS_CONSTRUCTOR) {
            return new ResolvedTarget(
                    SpringDependencyStatus.AMBIGUOUS_INJECTION,
                    Optional.empty(),
                    Optional.empty(),
                    candidate.targetQualifiedName());
        }
        if (candidate.status() != SpringInjectionStatus.CONFIRMED
                || candidate.targetQualifiedName().isEmpty()) {
            return new ResolvedTarget(
                    SpringDependencyStatus.UNRESOLVED_TARGET,
                    Optional.empty(),
                    Optional.empty(),
                    candidate.targetQualifiedName());
        }
        String resolvedType = candidate.targetQualifiedName().orElseThrow();
        List<SpringComponentFact> targets = componentsByName.getOrDefault(resolvedType, List.of());
        if (targets.size() == 1) {
            SpringComponentFact target = targets.getFirst();
            return new ResolvedTarget(
                    SpringDependencyStatus.CONFIRMED,
                    Optional.of(target.modulePomFileId()),
                    Optional.of(target.qualifiedName()),
                    Optional.of(resolvedType));
        }
        return new ResolvedTarget(
                targets.size() > 1
                        ? SpringDependencyStatus.AMBIGUOUS_TARGET
                        : SpringDependencyStatus.UNRESOLVED_TARGET,
                Optional.empty(),
                Optional.empty(),
                Optional.of(resolvedType));
    }

    private static Diagnostic diagnostic(SpringComponentDependencyFact dependency) {
        String code;
        String message;
        switch (dependency.status()) {
            case AMBIGUOUS_INJECTION -> {
                code = "SPRING_DEPENDENCY_INJECTION_AMBIGUOUS";
                message = "The injection point belongs to an ambiguous constructor selection; no confirmed edge exists.";
            }
            case AMBIGUOUS_TARGET -> {
                code = "SPRING_DEPENDENCY_TARGET_AMBIGUOUS";
                message = "The injected type matches multiple component declarations; no target was selected.";
            }
            case UNRESOLVED_TARGET -> {
                code = "SPRING_DEPENDENCY_TARGET_UNRESOLVED";
                message = "The injected type is missing or is not one uniquely confirmed component.";
            }
            case CONFIRMED -> throw new IllegalArgumentException(
                    "Confirmed dependency does not require a diagnostic");
            default -> throw new IllegalStateException(
                    "Unexpected dependency status: " + dependency.status());
        }
        return new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(dependency.sourceModulePomFileId()),
                Optional.of(dependency.location().sourceFileId()),
                Optional.of(dependency.location()),
                message);
    }

    private static int compareLocation(SourceLocation left, SourceLocation right) {
        int file = left.sourceFileId().compareTo(right.sourceFileId());
        if (file != 0) {
            return file;
        }
        int line = Integer.compare(
                left.startLine().orElse(Integer.MAX_VALUE),
                right.startLine().orElse(Integer.MAX_VALUE));
        return line != 0 ? line : Integer.compare(
                left.startColumn().orElse(Integer.MAX_VALUE),
                right.startColumn().orElse(Integer.MAX_VALUE));
    }

    private record ComponentKey(String modulePomFileId, String qualifiedName) {
    }

    private record ResolvedTarget(
            SpringDependencyStatus status,
            Optional<String> modulePomFileId,
            Optional<String> qualifiedName,
            Optional<String> resolvedTypeQualifiedName) {
    }

    private record DependencyKey(
            String sourceModulePomFileId,
            String sourceQualifiedName,
            SpringDependencyKind kind,
            SpringDependencyStatus status,
            Optional<String> targetModulePomFileId,
            Optional<String> targetQualifiedName,
            Optional<String> resolvedTypeQualifiedName,
            String unresolvedDeclaredType) {

        static DependencyKey from(SpringInjectionCandidate candidate, ResolvedTarget target) {
            return new DependencyKey(
                    candidate.modulePomFileId(),
                    candidate.ownerQualifiedName(),
                    SpringDependencyKind.COMPONENT_INJECTION,
                    target.status(),
                    target.modulePomFileId(),
                    target.qualifiedName(),
                    target.resolvedTypeQualifiedName(),
                    target.status() == SpringDependencyStatus.CONFIRMED
                            ? ""
                            : candidate.declaredType());
        }
    }

    private static final class DependencyAccumulator {

        private final ResolvedTarget target;
        private final List<Evidence> evidence = new ArrayList<>();
        private final String sourceModulePomFileId;
        private final String sourceQualifiedName;
        private String declaredTargetType;
        private SourceLocation location;

        private DependencyAccumulator(SpringInjectionCandidate candidate, ResolvedTarget target) {
            this.target = target;
            sourceModulePomFileId = candidate.modulePomFileId();
            sourceQualifiedName = candidate.ownerQualifiedName();
            declaredTargetType = candidate.declaredType();
            location = candidate.location();
        }

        private void add(SpringInjectionCandidate candidate) {
            if (candidate.declaredType().compareTo(declaredTargetType) < 0) {
                declaredTargetType = candidate.declaredType();
            }
            if (compareLocation(candidate.location(), location) < 0) {
                location = candidate.location();
            }
            evidence.addAll(candidate.evidence());
        }

        private SpringComponentDependencyFact toFact() {
            List<Evidence> consolidatedEvidence = evidence.stream()
                    .distinct()
                    .sorted(Comparator.comparing((Evidence item) -> item.location().sourceFileId())
                            .thenComparingInt(item -> item.location().startLine()
                                    .orElse(Integer.MAX_VALUE))
                            .thenComparing(Evidence::type)
                            .thenComparing(Evidence::ruleId))
                    .toList();
            return new SpringComponentDependencyFact(
                    sourceModulePomFileId,
                    sourceQualifiedName,
                    target.modulePomFileId(),
                    target.qualifiedName(),
                    target.resolvedTypeQualifiedName(),
                    declaredTargetType,
                    SpringDependencyKind.COMPONENT_INJECTION,
                    target.status(),
                    location,
                    consolidatedEvidence);
        }
    }
}
