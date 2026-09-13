package io.github.zhancm.repoonboard.cli;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** Stable, non-secret CLI presentation for input failures and report diagnostics. */
final class CliErrorReporter {
    private CliErrorReporter() {
    }

    static void printError(
            PrintWriter err,
            String code,
            String stage,
            String context,
            String message,
            String action) {
        Objects.requireNonNull(err, "err");
        err.printf("ERROR [%s] stage=%s%n", code, stage);
        if (context != null && !context.isBlank()) {
            err.printf("  Context: %s%n", context);
        }
        err.printf("  Message: %s%n", message);
        err.printf("  Action: %s%n", action);
    }

    static void printAnalysisSummary(PrintWriter err, AnalysisStatus status) {
        switch (status) {
            case SUCCESS -> { }
            case PARTIAL -> {
                err.println("Analysis result: PARTIAL - confirmed facts were preserved, but coverage is incomplete.");
                err.println("Action: review WARNING diagnostics and source locations, fix relevant inputs, "
                        + "then rerun.");
            }
            case FAILED -> {
                err.println("Analysis result: FAILED - terminal diagnostics prevented a complete analysis.");
                err.println("Action: resolve ERROR diagnostics, then rerun RepoOnboard.");
            }
        }
    }

    static void printDiagnostics(PrintWriter err, List<Diagnostic> diagnostics) {
        for (Diagnostic diagnostic : diagnostics) {
            err.printf("%s [%s] stage=%s%n",
                    diagnostic.severity(), diagnostic.code(), diagnostic.stage());
            diagnostic.moduleId().ifPresent(module -> err.printf("  Module: %s%n", module));
            diagnostic.location()
                    .map(CliErrorReporter::formatLocation)
                    .or(() -> diagnostic.fileId())
                    .ifPresent(source -> err.printf("  Source: %s%n", source));
            err.printf("  Message: %s%n", diagnostic.message());
        }
    }

    private static String formatLocation(SourceLocation location) {
        StringBuilder value = new StringBuilder(location.sourceFileId());
        append(value, location.startLine());
        append(value, location.startColumn());
        return value.toString();
    }

    private static void append(StringBuilder value, OptionalInt part) {
        if (part.isPresent()) {
            value.append(':').append(part.getAsInt());
        }
    }
}
