package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds confirmed directed component edges from Spring injection candidates. */
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

        List<SpringComponentDependencyFact> dependencies = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (SpringInjectionCandidate candidate : injectionAnalysis.candidates()) {
            if (!components.containsKey(new ComponentKey(
                    candidate.modulePomFileId(), candidate.ownerQualifiedName()))) {
                continue;
            }
            if (candidate.status() != SpringInjectionStatus.CONFIRMED
                    || candidate.targetQualifiedName().isEmpty()) {
                diagnostics.add(unresolvedDiagnostic(candidate));
                continue;
            }
            List<SpringComponentFact> targets = componentsByName.getOrDefault(
                    candidate.targetQualifiedName().orElseThrow(), List.of());
            if (targets.size() != 1) {
                diagnostics.add(unresolvedDiagnostic(candidate));
                continue;
            }
            SpringComponentFact target = targets.getFirst();
            dependencies.add(new SpringComponentDependencyFact(
                    candidate.modulePomFileId(),
                    candidate.ownerQualifiedName(),
                    Optional.of(target.modulePomFileId()),
                    Optional.of(target.qualifiedName()),
                    candidate.declaredType(),
                    SpringDependencyKind.COMPONENT_INJECTION,
                    SpringDependencyStatus.CONFIRMED,
                    candidate.location(),
                    candidate.evidence()));
        }
        dependencies.sort(Comparator.comparing(SpringComponentDependencyFact::sourceModulePomFileId)
                .thenComparing(SpringComponentDependencyFact::sourceQualifiedName)
                .thenComparing(dependency -> dependency.targetModulePomFileId().orElse(""))
                .thenComparing(dependency -> dependency.targetQualifiedName().orElse(""))
                .thenComparingInt(dependency -> dependency.location().startLine()
                        .orElse(Integer.MAX_VALUE)));
        return new SpringComponentDependencyAnalysis(dependencies, diagnostics);
    }

    private static Diagnostic unresolvedDiagnostic(SpringInjectionCandidate candidate) {
        return new Diagnostic(
                "SPRING_COMPONENT_DEPENDENCY_UNCONFIRMED",
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(candidate.modulePomFileId()),
                Optional.of(candidate.location().sourceFileId()),
                Optional.of(candidate.location()),
                "The injection target is not one uniquely confirmed Spring component; no dependency edge was created.");
    }

    private record ComponentKey(String modulePomFileId, String qualifiedName) {
    }
}
