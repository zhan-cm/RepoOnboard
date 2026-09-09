package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Spring configuration and application-entry facts with diagnostics. */
public record SpringConfigurationAnalysis(
        List<SpringConfigurationFact> configurations,
        List<SpringEntryPointFact> entryPoints,
        List<Diagnostic> diagnostics) {

    public SpringConfigurationAnalysis {
        configurations = List.copyOf(Objects.requireNonNull(configurations, "configurations"));
        entryPoints = List.copyOf(Objects.requireNonNull(entryPoints, "entryPoints"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
