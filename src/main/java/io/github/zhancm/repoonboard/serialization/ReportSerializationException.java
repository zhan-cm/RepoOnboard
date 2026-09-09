package io.github.zhancm.repoonboard.serialization;

/** Indicates that a report could not be encoded or decoded safely. */
public class ReportSerializationException extends RuntimeException {
    public ReportSerializationException(String message) {
        super(message);
    }

    public ReportSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
