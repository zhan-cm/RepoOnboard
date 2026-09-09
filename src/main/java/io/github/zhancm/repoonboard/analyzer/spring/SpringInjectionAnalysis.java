package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Spring injection candidates and explicit limitations. */
public record SpringInjectionAnalysis(
        List<SpringInjectionCandidate> candidates,
        List<Diagnostic> diagnostics) {

    public SpringInjectionAnalysis {
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
