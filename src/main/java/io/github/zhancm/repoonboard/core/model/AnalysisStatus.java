package io.github.zhancm.repoonboard.core.model;

import java.util.Collection;
import java.util.Objects;

/** Overall completeness of an analysis result. */
public enum AnalysisStatus {
    SUCCESS,
    PARTIAL,
    FAILED;

    /**
     * Derives status from diagnostic severity.
     *
     * <p>No diagnostics, or informational diagnostics only, produce
     * {@link #SUCCESS}. A warning produces {@link #PARTIAL}. An error always
     * produces {@link #FAILED} and takes precedence over warnings.</p>
     */
    public static AnalysisStatus fromDiagnostics(Collection<Diagnostic> diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");
        AnalysisStatus status = SUCCESS;
        for (Diagnostic diagnostic : diagnostics) {
            DiagnosticSeverity severity = Objects.requireNonNull(diagnostic, "diagnostic")
                    .severity();
            if (severity == DiagnosticSeverity.ERROR) {
                return FAILED;
            }
            if (severity == DiagnosticSeverity.WARNING) {
                status = PARTIAL;
            }
        }
        return status;
    }
}
