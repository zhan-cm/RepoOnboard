package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Component dependency facts plus recoverable diagnostics. */
public record SpringComponentDependencyAnalysis(
        List<SpringComponentDependencyFact> dependencies,
        List<Diagnostic> diagnostics) {

    public SpringComponentDependencyAnalysis {
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }

    /** Builds a stable adjacency list containing confirmed component edges only. */
    public Map<SpringComponentRef, List<SpringComponentRef>> confirmedAdjacency() {
        Map<SpringComponentRef, List<SpringComponentRef>> adjacency = new LinkedHashMap<>();
        dependencies.stream()
                .filter(dependency -> dependency.status() == SpringDependencyStatus.CONFIRMED)
                .forEach(dependency -> {
                    SpringComponentRef source = new SpringComponentRef(
                            dependency.sourceModulePomFileId(), dependency.sourceQualifiedName());
                    SpringComponentRef target = new SpringComponentRef(
                            dependency.targetModulePomFileId().orElseThrow(),
                            dependency.targetQualifiedName().orElseThrow());
                    adjacency.computeIfAbsent(source, ignored -> new ArrayList<>()).add(target);
                    adjacency.computeIfAbsent(target, ignored -> new ArrayList<>());
                });
        Map<SpringComponentRef, List<SpringComponentRef>> sorted = new LinkedHashMap<>();
        adjacency.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(
                        entry.getKey(), entry.getValue().stream().distinct().sorted().toList()));
        return Collections.unmodifiableMap(sorted);
    }
}
