package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Spring component facts and recoverable classification diagnostics. */
public record SpringComponentAnalysis(
        List<SpringComponentFact> components,
        List<Diagnostic> diagnostics) {

    public SpringComponentAnalysis {
        components = List.copyOf(Objects.requireNonNull(components, "components"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
