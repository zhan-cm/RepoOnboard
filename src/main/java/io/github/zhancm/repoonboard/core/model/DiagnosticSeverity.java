package io.github.zhancm.repoonboard.core.model;

/**
 * The effect of a diagnostic on the completeness of an analysis.
 *
 * <p>{@link #INFO} preserves a successful result, {@link #WARNING} identifies
 * a recoverable limitation that makes the result partial, and {@link #ERROR}
 * identifies a terminal problem that makes the analysis fail.</p>
 */
public enum DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR
}
