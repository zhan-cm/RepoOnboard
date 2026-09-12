package io.github.zhancm.repoonboard.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.BuildSystem;
import io.github.zhancm.repoonboard.core.model.EntryPoint;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.Framework;
import io.github.zhancm.repoonboard.core.model.Module;
import io.github.zhancm.repoonboard.core.model.Project;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class StartHereJsonTest {
    @Test
    void serializesStableReadingOrderReasonsAndSnapshotIdentity() throws Exception {
        AnalysisReport report = report();

        var root = JsonMapper.builder().build().readValue(
                new StartHereJson().serialize(report), JsonNode.class);

        assertEquals("1.0", root.get("schemaVersion").textValue());
        assertEquals(AnalysisReportJson.currentSchemaVersion(),
                root.get("reportSchemaVersion").textValue());
        assertEquals(report.project().id(), root.get("projectId").textValue());
        assertEquals("SUCCESS", root.get("analysisStatus").textValue());
        assertEquals(2, root.get("totalItemCount").intValue());
        assertEquals("pom.xml", root.get("items").get(0).get("sourceFileId").textValue());
        assertEquals("ROOT_BUILD_FILE",
                root.get("items").get(0).get("reasons").get(0).get("kind").textValue());
        assertEquals("src/main/java/example/Application.java",
                root.get("items").get(1).get("sourceFileId").textValue());
        assertEquals("APPLICATION_ENTRY_POINT",
                root.get("items").get(1).get("reasons").get(0).get("kind").textValue());
        assertTrue(root.get("coverageNotice").isNull());
    }

    private static AnalysisReport report() {
        SourceLocation pom = SourceLocation.file("pom.xml");
        SourceLocation application = SourceLocation.file(
                "src/main/java/example/Application.java");
        Evidence mavenEvidence = new Evidence(
                "MAVEN_PROJECT", pom, List.of(), "maven.project.root");
        Evidence entryEvidence = new Evidence(
                "SPRING_APPLICATION", application, List.of(), "spring.application.entry");
        Module module = new Module(
                "module:root", "pom.xml", ".", Optional.empty(),
                Optional.of("example"), Optional.of("demo"), Optional.empty(), Optional.empty(),
                List.of("src/main/java"), List.of(Framework.SPRING_BOOT),
                List.of(), List.of(), List.of(mavenEvidence));
        EntryPoint entryPoint = new EntryPoint(
                "entry:application", module.id(), "example.Application",
                "APPLICATION_ENTRY_POINT", Framework.SPRING_BOOT,
                application, List.of(entryEvidence));
        return new AnalysisReport(
                new Project("project:demo", "demo", BuildSystem.MAVEN, List.of(mavenEvidence)),
                List.of(module), List.of(), List.of(), List.of(), List.of(entryPoint), List.of(),
                AnalysisStatus.SUCCESS, List.of());
    }
}
