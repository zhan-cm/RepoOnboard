package io.github.zhancm.repoonboard.analyzer.maven;

/** Maven build declarations that directly establish Spring Boot usage. */
public enum SpringBootBuildEvidenceKind {
    PARENT,
    BOM,
    DEPENDENCY
}
