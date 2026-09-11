package io.github.zhancm.repoonboard.analysis;

/** Explicit, explainable rules used to identify important repository files. */
public enum ReadingImportanceRule {
    ROOT_POM,
    APPLICATION_ENTRY_POINT,
    CONFIGURATION,
    API_CONTROLLER,
    CONFIRMED_DEPENDENCY_NEIGHBOR
}
