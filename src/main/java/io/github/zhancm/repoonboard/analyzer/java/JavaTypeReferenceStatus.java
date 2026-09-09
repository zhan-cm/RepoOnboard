package io.github.zhancm.repoonboard.analyzer.java;

/** Whether a source type reference has one confirmed project-local target. */
public enum JavaTypeReferenceStatus {
    RESOLVED,
    UNRESOLVED,
    AMBIGUOUS
}
