package io.github.zhancm.repoonboard.serialization;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/** UTF-8 JSON codec for the versioned public analysis report. */
public final class AnalysisReportJson {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .deactivateDefaultTyping()
            .build();
    private static final ObjectWriter WRITER = MAPPER.writer().with(prettyPrinter());

    /** Returns the schema version emitted by this build. */
    public static String currentSchemaVersion() {
        return ReportSchemaVersion.CURRENT.toString();
    }

    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(new DefaultIndenter("  ", "\n"));
        return printer;
    }

    /** Serializes a report as deterministic, human-readable JSON. */
    public String serialize(AnalysisReport report) {
        try {
            return WRITER.writeValueAsString(ReportJsonDocument.fromModel(report));
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new ReportSerializationException("Could not serialize analysis report", exception);
        }
    }

    /** Deserializes JSON after validating its schema major version. */
    public AnalysisReport deserialize(String json) {
        Objects.requireNonNull(json, "json");
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root == null || !root.isObject()) {
                throw new ReportSerializationException("Analysis report JSON must be an object");
            }
            JsonNode versionNode = root.get("schemaVersion");
            if (versionNode == null || !versionNode.isTextual()) {
                throw new ReportSerializationException(
                        "Analysis report JSON requires a textual schemaVersion");
            }
            ReportSchemaVersion actualVersion = ReportSchemaVersion.parse(versionNode.textValue());
            if (!actualVersion.isCompatibleWith(ReportSchemaVersion.CURRENT)) {
                throw new UnsupportedReportSchemaVersionException(
                        actualVersion,
                        ReportSchemaVersion.CURRENT);
            }
            return MAPPER.treeToValue(root, ReportJsonDocument.class).toModel();
        } catch (ReportSerializationException exception) {
            throw exception;
        } catch (JacksonException | IllegalArgumentException | NullPointerException exception) {
            throw new ReportSerializationException("Could not deserialize analysis report", exception);
        }
    }

    /** Writes a report using UTF-8 without relying on the platform default charset. */
    public void write(Path output, AnalysisReport report) {
        Objects.requireNonNull(output, "output");
        try {
            Files.write(output, serialize(report).getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new ReportSerializationException("Could not write analysis report: " + output, exception);
        }
    }

    /** Reads a UTF-8 report from disk. */
    public AnalysisReport read(Path input) {
        Objects.requireNonNull(input, "input");
        try {
            return deserialize(Files.readString(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new ReportSerializationException("Could not read analysis report: " + input, exception);
        }
    }
}
