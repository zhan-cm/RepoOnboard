package io.github.zhancm.repoonboard.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.BuildSystem;
import io.github.zhancm.repoonboard.core.model.Component;
import io.github.zhancm.repoonboard.core.model.ComponentKind;
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.Endpoint;
import io.github.zhancm.repoonboard.core.model.EndpointConditions;
import io.github.zhancm.repoonboard.core.model.EntryPoint;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.Framework;
import io.github.zhancm.repoonboard.core.model.FrameworkVersion;
import io.github.zhancm.repoonboard.core.model.Language;
import io.github.zhancm.repoonboard.core.model.LanguageVersion;
import io.github.zhancm.repoonboard.core.model.Module;
import io.github.zhancm.repoonboard.core.model.Project;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import io.github.zhancm.repoonboard.core.model.SourceFile;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalysisReportJsonTest {
    private final AnalysisReportJson codec = new AnalysisReportJson();

    @Test
    void serializesAStableHumanReadableSnapshot() {
        AnalysisReport minimal = minimalReport();

        String expected = """
                {
                  "schemaVersion" : "1.2",
                  "project" : {
                    "id" : "project:demo",
                    "name" : "demo",
                    "buildSystem" : "MAVEN",
                    "evidence" : [ ]
                  },
                  "summary" : {
                    "moduleCount" : 0,
                    "sourceFileCount" : 0,
                    "componentCount" : 0,
                    "controllerCount" : 0,
                    "serviceCount" : 0,
                    "repositoryCount" : 0,
                    "configurationCount" : 0,
                    "endpointCount" : 0,
                    "entryPointCount" : 0,
                    "dependencyCount" : 0,
                    "analysisStatus" : "SUCCESS",
                    "coverageLimited" : false,
                    "coverageLimitationCodes" : [ ]
                  },
                  "modules" : [ ],
                  "sourceFiles" : [ ],
                  "components" : [ ],
                  "endpoints" : [ ],
                  "entryPoints" : [ ],
                  "dependencies" : [ ],
                  "status" : "SUCCESS",
                  "diagnostics" : [ ]
                }
                """.stripTrailing();

        assertEquals(expected, codec.serialize(minimal));
    }

    @Test
    void roundTripRetainsEveryPublicEntityAndSourceLocation() {
        AnalysisReport report = completeReport();

        String json = codec.serialize(report);

        assertEquals(report, codec.deserialize(json));
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("\"sourceFileId\" : \"src/main/java/example/UserController.java\""));
        assertTrue(json.contains("\"startLine\" : 12"));
        assertTrue(json.contains("\"symbol\" : \"example.UserController.find\""));
        assertTrue(json.contains("\"controllerCount\" : 1"));
        assertTrue(json.contains("\"coverageLimited\" : true"));
        assertTrue(json.contains("\"coverageLimitationCodes\" : [ \"JAVA_PARTIAL\" ]"));
    }

    @Test
    void readsAndWritesUtf8WithoutUsingThePlatformCharset(@TempDir Path tempDirectory) {
        Path reportFile = tempDirectory.resolve("report.json");
        AnalysisReport report = completeReport();

        codec.write(reportFile, report);

        assertEquals(report, codec.read(reportFile));
        assertTrue(codec.serialize(report).contains("示例项目"));
    }

    @Test
    void acceptsAdditiveMinorVersionsAndIgnoresUnknownFieldsWithoutTypeActivation() {
        AnalysisReport report = completeReport();
        String json = codec.serialize(report)
                .replace("\"schemaVersion\" : \"1.2\"", "\"schemaVersion\" : \"1.9\"")
                .replaceFirst("\\{", "{\n  \"@class\" : \"java.lang.Runtime\",\n"
                        + "  \"futureConfiguration\" : { \"secret\" : \"not-loaded\" },");

        assertEquals(report, codec.deserialize(json));
    }

    @Test
    void rejectsUnknownMajorVersionBeforeBindingTheReport() {
        String json = codec.serialize(completeReport())
                .replace("\"schemaVersion\" : \"1.2\"", "\"schemaVersion\" : \"2.0\"");

        UnsupportedReportSchemaVersionException exception = assertThrows(
                UnsupportedReportSchemaVersionException.class,
                () -> codec.deserialize(json));

        assertEquals(new ReportSchemaVersion(2, 0), exception.actualVersion());
        assertEquals(ReportSchemaVersion.CURRENT, exception.supportedVersion());
        assertTrue(exception.getMessage().contains("supported major version is 1"));
    }

    @Test
    void rejectsMissingMalformedAndTrailingJson() {
        assertThrows(
                ReportSerializationException.class,
                () -> codec.deserialize("{\"project\":{}}"));
        assertThrows(
                ReportSerializationException.class,
                () -> codec.deserialize("{\"schemaVersion\":\"v1\"}"));
        assertThrows(
                ReportSerializationException.class,
                () -> codec.deserialize(codec.serialize(completeReport()) + " {}"));
        String duplicateVersion = codec.serialize(completeReport()).replace(
                "\"schemaVersion\" : \"1.2\"",
                "\"schemaVersion\" : \"2.0\",\n  \"schemaVersion\" : \"1.2\"");
        assertThrows(
                ReportSerializationException.class,
                () -> codec.deserialize(duplicateVersion));
    }

    @Test
    void validatesRelativePathsAgainWhenReadingUntrustedJson() {
        String json = codec.serialize(completeReport()).replace(
                "\"sourceFileId\" : \"src/main/java/example/UserController.java\"",
                "\"sourceFileId\" : \"C:/Users/example/UserController.java\"");

        assertThrows(ReportSerializationException.class, () -> codec.deserialize(json));
    }

    @Test
    void defaultJsonContainsNoSourceBodyAbsoluteRootOrPolymorphicMetadata() {
        String json = codec.serialize(completeReport());

        assertFalse(json.contains("sourceBody"));
        assertFalse(json.contains("sourceCode"));
        assertFalse(json.contains("C:\\\\Users"));
        assertFalse(json.contains("E:\\\\Projects"));
        assertFalse(json.contains("@class"));
        assertFalse(json.contains("@type"));
    }

    @Test
    void readsLegacyVersionOneWithoutTheAdditiveSummaryField() {
        String legacySummaryBlock = """
                  "summary" : {
                    "moduleCount" : 0,
                    "sourceFileCount" : 0,
                    "componentCount" : 0,
                    "controllerCount" : 0,
                    "serviceCount" : 0,
                    "repositoryCount" : 0,
                    "configurationCount" : 0,
                    "endpointCount" : 0,
                    "entryPointCount" : 0,
                    "dependencyCount" : 0,
                    "analysisStatus" : "SUCCESS",
                    "coverageLimited" : false,
                    "coverageLimitationCodes" : [ ]
                  },
                """;
        String json = codec.serialize(minimalReport())
                .replace("\"schemaVersion\" : \"1.2\"", "\"schemaVersion\" : \"1.0\"")
                .replace(legacySummaryBlock, "");

        assertEquals(minimalReport(), codec.deserialize(json));
    }

    @Test
    void readsVersionOnePointOneModulesWithoutVersionAndHierarchyFields() {
        AnalysisReport legacy = legacyModuleReport();
        String json = codec.serialize(legacy)
                .replace("\"schemaVersion\" : \"1.2\"", "\"schemaVersion\" : \"1.1\"")
                .replace("      \"aggregationParentModuleId\" : null,\n", "")
                .replace("      \"languageVersions\" : [ ],\n", "")
                .replace("      \"frameworkVersions\" : [ ],\n", "");

        assertEquals(legacy, codec.deserialize(json));
    }

    @Test
    void rejectsASummaryThatDoesNotMatchItsEntities() {
        String json = codec.serialize(completeReport())
                .replace("\"moduleCount\" : 1", "\"moduleCount\" : 99");

        assertThrows(ReportSerializationException.class, () -> codec.deserialize(json));
    }

    @Test
    void schemaVersionUsesStrictMajorMinorSyntax() {
        assertEquals("1.2", ReportSchemaVersion.CURRENT.toString());
        assertEquals(new ReportSchemaVersion(12, 34), ReportSchemaVersion.parse("12.34"));
        assertTrue(new ReportSchemaVersion(1, 99).isCompatibleWith(ReportSchemaVersion.CURRENT));
        assertFalse(new ReportSchemaVersion(2, 0).isCompatibleWith(ReportSchemaVersion.CURRENT));
        assertThrows(IllegalArgumentException.class, () -> ReportSchemaVersion.parse("01.0"));
        assertThrows(IllegalArgumentException.class, () -> ReportSchemaVersion.parse("1"));
    }

    private static AnalysisReport minimalReport() {
        return new AnalysisReport(
                new Project("project:demo", "demo", BuildSystem.MAVEN, List.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                AnalysisStatus.SUCCESS,
                List.of());
    }

    private static AnalysisReport completeReport() {
        SourceLocation pom = SourceLocation.file("pom.xml");
        SourceLocation source = new SourceLocation(
                "src/main/java/example/UserController.java",
                OptionalInt.of(12),
                OptionalInt.of(5),
                OptionalInt.of(14),
                OptionalInt.of(6),
                Optional.of("example.UserController.find"));
        Evidence mavenEvidence = new Evidence("MAVEN_MODEL", pom, List.of(), "MAVEN_PROJECT");
        Evidence sourceEvidence = new Evidence(
                "SPRING_ANNOTATION",
                source,
                List.of(pom),
                "SPRING_REST_CONTROLLER");

        Project project = new Project(
                "project:example",
                "示例项目",
                BuildSystem.MAVEN,
                List.of(mavenEvidence));
        Module module = new Module(
                "module:.",
                "pom.xml",
                ".",
                Optional.empty(),
                Optional.of("example"),
                Optional.of("demo"),
                Optional.of("1.0.0"),
                Optional.of("jar"),
                List.of("src/main/java"),
                List.of(Framework.SPRING_BOOT),
                List.of(new LanguageVersion(Language.JAVA, "21", List.of(mavenEvidence))),
                List.of(new FrameworkVersion(
                        Framework.SPRING_BOOT, "3.5.0", List.of(mavenEvidence))),
                List.of(mavenEvidence));
        SourceFile sourceFile = new SourceFile(
                "source:controller",
                module.id(),
                source.sourceFileId(),
                Language.JAVA,
                source);
        Component component = new Component(
                "component:user-controller",
                module.id(),
                "example.UserController",
                Optional.of("userController"),
                ComponentKind.REST_CONTROLLER,
                Framework.SPRING_BOOT,
                source,
                List.of(sourceEvidence));
        Endpoint endpoint = new Endpoint(
                "endpoint:find-user",
                module.id(),
                component.id(),
                "GET",
                Optional.of("/users/{id}"),
                false,
                "find(java.lang.String)",
                new EndpointConditions(
                        List.of("active=true"),
                        List.of("X-Tenant"),
                        List.of(),
                        List.of("application/json"),
                        false),
                Framework.SPRING_BOOT,
                source,
                List.of(sourceEvidence));
        EntryPoint entryPoint = new EntryPoint(
                "entry:application",
                module.id(),
                "example.Application",
                "APPLICATION",
                Framework.SPRING_BOOT,
                source,
                List.of(sourceEvidence));
        Dependency dependency = new Dependency(
                "dependency:service",
                component.id(),
                Optional.of("component:user-service"),
                "example.UserService",
                DependencyKind.COMPONENT_INJECTION,
                ResolutionStatus.CONFIRMED,
                source,
                List.of(sourceEvidence));
        Diagnostic diagnostic = new Diagnostic(
                "JAVA_PARTIAL",
                DiagnosticSeverity.WARNING,
                "JAVA",
                Optional.of(module.id()),
                Optional.of(source.sourceFileId()),
                Optional.of(source),
                "保留部分分析结果");

        return new AnalysisReport(
                project,
                List.of(module),
                List.of(sourceFile),
                List.of(component),
                List.of(endpoint),
                List.of(entryPoint),
                List.of(dependency),
                AnalysisStatus.PARTIAL,
                List.of(diagnostic));
    }

    private static AnalysisReport legacyModuleReport() {
        Module module = new Module(
                "module:.", "pom.xml", ".", Optional.empty(),
                Optional.of("example"), Optional.of("legacy"), Optional.of("1"),
                Optional.of("jar"), List.of(), List.of(), List.of(), List.of(),
                List.of(new Evidence(
                        "MAVEN_MODULE", SourceLocation.file("pom.xml"), List.of(), "maven.module")));
        return new AnalysisReport(
                new Project("project:legacy", "legacy", BuildSystem.MAVEN, List.of()),
                List.of(module), List.of(), List.of(), List.of(), List.of(), List.of(),
                AnalysisStatus.SUCCESS, List.of());
    }
}
