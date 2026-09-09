package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Java source files and recoverable traversal diagnostics. */
public record JavaFileDiscovery(List<JavaSourceFile> files, List<Diagnostic> diagnostics) {

    public JavaFileDiscovery {
        files = List.copyOf(Objects.requireNonNull(files, "files"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }
}
