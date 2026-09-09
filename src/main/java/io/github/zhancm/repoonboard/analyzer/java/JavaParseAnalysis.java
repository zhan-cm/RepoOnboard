package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Parsed Java compilation-unit facts plus recoverable diagnostics. */
public record JavaParseAnalysis(
        List<JavaCompilationUnitFact> compilationUnits,
        List<Diagnostic> diagnostics) {

    public JavaParseAnalysis {
        compilationUnits = List.copyOf(Objects.requireNonNull(compilationUnits, "compilationUnits"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
