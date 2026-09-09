package io.github.zhancm.repoonboard.analyzer.spring;

/** Confidence of an injection candidate before dependency-edge construction. */
public enum SpringInjectionStatus {
    CONFIRMED,
    AMBIGUOUS_CONSTRUCTOR,
    UNRESOLVED_TYPE
}
