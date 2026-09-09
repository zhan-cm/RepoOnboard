package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
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
}
