package io.github.zhancm.repoonboard.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalysisStatusTest {

    @Test
    void noDiagnosticsOrInformationOnlyIsSuccessful() {
        assertEquals(AnalysisStatus.SUCCESS, AnalysisStatus.fromDiagnostics(List.of()));
        assertEquals(
                AnalysisStatus.SUCCESS,
                AnalysisStatus.fromDiagnostics(List.of(diagnostic(DiagnosticSeverity.INFO))));
    }

    @Test
    void aRecoverableWarningMakesTheResultPartial() {
        assertEquals(
                AnalysisStatus.PARTIAL,
                AnalysisStatus.fromDiagnostics(List.of(diagnostic(DiagnosticSeverity.WARNING))));
    }

    @Test
    void aTerminalErrorMakesTheResultFailedAndDominatesWarnings() {
        assertEquals(
                AnalysisStatus.FAILED,
                AnalysisStatus.fromDiagnostics(List.of(
                        diagnostic(DiagnosticSeverity.WARNING),
                        diagnostic(DiagnosticSeverity.ERROR))));
    }

    private static Diagnostic diagnostic(DiagnosticSeverity severity) {
        return new Diagnostic(
                "TEST_DIAGNOSTIC",
                severity,
                "TEST",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "Test diagnostic.");
    }
}
