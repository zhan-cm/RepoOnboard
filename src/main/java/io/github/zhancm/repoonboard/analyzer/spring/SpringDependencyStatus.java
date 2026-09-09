package io.github.zhancm.repoonboard.analyzer.spring;

/** Confidence state of a component dependency relationship. */
public enum SpringDependencyStatus {
    CONFIRMED,
    AMBIGUOUS_INJECTION,
    UNRESOLVED_TARGET
}
