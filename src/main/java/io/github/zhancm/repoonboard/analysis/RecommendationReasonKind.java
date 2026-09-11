package io.github.zhancm.repoonboard.analysis;

/** Stable reason categories behind a recommended source file. */
public enum RecommendationReasonKind {
    ROOT_BUILD_FILE,
    APPLICATION_ENTRY_POINT,
    CONFIGURATION_COMPONENT,
    HTTP_ENDPOINT_EXPOSURE,
    CONFIRMED_DEPENDENCY_PATH,
    CONFIRMED_INCOMING_DEPENDENCIES,
    CONFIRMED_OUTGOING_DEPENDENCIES
}
