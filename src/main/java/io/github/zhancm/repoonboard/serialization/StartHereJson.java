package io.github.zhancm.repoonboard.serialization;

import io.github.zhancm.repoonboard.analysis.RecommendationExplanationGenerator;
import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/** Deterministic JSON codec for the local Start Here projection. */
public final class StartHereJson {
    private static final String SCHEMA_VERSION = "1.0";
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .deactivateDefaultTyping()
            .build();
    private static final ObjectWriter WRITER = MAPPER.writer().with(prettyPrinter());

    public static String currentSchemaVersion() {
        return SCHEMA_VERSION;
    }

    /** Generates and serializes a Start Here projection from one immutable report snapshot. */
    public String serialize(AnalysisReport report) {
        Objects.requireNonNull(report, "report");
        try {
            var explainedPath = new RecommendationExplanationGenerator().generate(report);
            return WRITER.writeValueAsString(
                    StartHereJsonDocument.fromModel(report, explainedPath));
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new ReportSerializationException(
                    "Could not serialize Start Here projection", exception);
        }
    }

    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(new DefaultIndenter("  ", "\n"));
        return printer;
    }
}
