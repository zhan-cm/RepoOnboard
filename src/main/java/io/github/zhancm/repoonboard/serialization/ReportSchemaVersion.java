package io.github.zhancm.repoonboard.serialization;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Major/minor version of the public analysis report JSON schema. */
public record ReportSchemaVersion(int major, int minor) {
    public static final ReportSchemaVersion CURRENT = new ReportSchemaVersion(1, 2);

    private static final Pattern FORMAT = Pattern.compile("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)");

    public ReportSchemaVersion {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("Schema version numbers must not be negative");
        }
    }

    /** Parses the canonical {@code major.minor} representation. */
    public static ReportSchemaVersion parse(String value) {
        Objects.requireNonNull(value, "value");
        Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "schemaVersion must use the canonical major.minor format");
        }
        try {
            return new ReportSchemaVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("schemaVersion is outside the supported range", exception);
        }
    }

    /** Minor versions are additive and compatible within the same major version. */
    public boolean isCompatibleWith(ReportSchemaVersion supportedVersion) {
        return major == Objects.requireNonNull(supportedVersion, "supportedVersion").major;
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
