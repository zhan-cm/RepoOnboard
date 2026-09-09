package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Resolved Spring MVC endpoints plus recoverable diagnostics. */
public record SpringEndpointAnalysis(
        List<SpringEndpointFact> endpoints,
        List<Diagnostic> diagnostics) {

    public SpringEndpointAnalysis {
        endpoints = List.copyOf(Objects.requireNonNull(endpoints, "endpoints"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
