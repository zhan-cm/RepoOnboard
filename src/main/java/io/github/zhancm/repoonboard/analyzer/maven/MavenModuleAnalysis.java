package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MavenModuleAnalysis(Optional<MavenModule> root, List<Diagnostic> diagnostics) {
    public MavenModuleAnalysis {
        Objects.requireNonNull(root, "root");
        diagnostics = List.copyOf(diagnostics);
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
