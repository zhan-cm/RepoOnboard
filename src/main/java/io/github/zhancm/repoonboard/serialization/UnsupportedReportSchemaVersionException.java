package io.github.zhancm.repoonboard.serialization;

/** Indicates that a JSON report uses an unsupported schema major version. */
public final class UnsupportedReportSchemaVersionException extends ReportSerializationException {
    private final ReportSchemaVersion actualVersion;
    private final ReportSchemaVersion supportedVersion;

    public UnsupportedReportSchemaVersionException(
            ReportSchemaVersion actualVersion,
            ReportSchemaVersion supportedVersion) {
        super("Unsupported report schema major version " + actualVersion.major()
                + "; supported major version is " + supportedVersion.major());
        this.actualVersion = actualVersion;
        this.supportedVersion = supportedVersion;
    }

    public ReportSchemaVersion actualVersion() {
        return actualVersion;
    }

    public ReportSchemaVersion supportedVersion() {
        return supportedVersion;
    }
}
