package io.github.zhancm.repoonboard.analyzer.maven;

final class RestrictedPomException extends Exception {

    private final String code;
    private final String sourceFileId;

    RestrictedPomException(String code, String sourceFileId, String message) {
        super(message);
        this.code = code;
        this.sourceFileId = sourceFileId;
    }

    String code() {
        return code;
    }

    String sourceFileId() {
        return sourceFileId;
    }
}
