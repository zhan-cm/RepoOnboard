package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Spring MVC mapping declarations plus recoverable diagnostics. */
public record SpringMappingAnalysis(
        List<SpringMappingFact> mappings,
        List<Diagnostic> diagnostics) {

    public SpringMappingAnalysis {
        mappings = List.copyOf(Objects.requireNonNull(mappings, "mappings"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
