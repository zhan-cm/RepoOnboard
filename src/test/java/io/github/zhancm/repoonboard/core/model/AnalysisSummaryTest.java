package io.github.zhancm.repoonboard.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalysisSummaryTest {
    private static final SourceLocation POM = SourceLocation.file("pom.xml");
    private static final SourceLocation SOURCE = SourceLocation.file("src/main/java/example/App.java");
    private static final Evidence EVIDENCE = new Evidence(
            "DECLARATION",
            SOURCE,
            List.of(),
            "test.declaration");

    @Test
    void derivesCountsFromUniqueStableEntityIdentities() {
        Module module = new Module(
                "module:root", "pom.xml", ".", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), List.of("src/main/java"), List.of(), List.of());
        SourceFile sourceFile = new SourceFile(
                "source:app", module.id(), SOURCE.sourceFileId(), Language.JAVA, SOURCE);
        Component restController = component("component:rest", ComponentKind.REST_CONTROLLER);
        Component controller = component("component:mvc", ComponentKind.CONTROLLER);
        Component service = component("component:service", ComponentKind.SERVICE);
        Component repository = component("component:repository", ComponentKind.REPOSITORY);
        Component configuration = component("component:configuration", ComponentKind.CONFIGURATION);
        Endpoint endpoint = new Endpoint(
                "endpoint:list", module.id(), restController.id(), "GET", Optional.of("/items"),
                false, "list()", new EndpointConditions(List.of(), List.of(), List.of(), List.of(), false),
                Framework.SPRING_BOOT, SOURCE, List.of(EVIDENCE));
        EntryPoint entryPoint = new EntryPoint(
                "entry:application", module.id(), "example.App", "APPLICATION",
                Framework.SPRING_BOOT, SOURCE, List.of(EVIDENCE));
        Dependency dependency = new Dependency(
                "dependency:service", restController.id(), Optional.of(service.id()),
                "example.Service", DependencyKind.COMPONENT_INJECTION,
                ResolutionStatus.CONFIRMED, SOURCE, List.of(EVIDENCE));
        Diagnostic info = diagnostic("INFO_ONLY", DiagnosticSeverity.INFO);
        Diagnostic firstWarning = diagnostic("Z_WARNING", DiagnosticSeverity.WARNING);
        Diagnostic repeatedWarning = diagnostic("Z_WARNING", DiagnosticSeverity.WARNING);
        Diagnostic secondWarning = diagnostic("A_WARNING", DiagnosticSeverity.WARNING);

        AnalysisReport report = new AnalysisReport(
                new Project("project:demo", "demo", BuildSystem.MAVEN, List.of()),
                List.of(module, module),
                List.of(sourceFile, sourceFile),
                List.of(
                        restController, restController, controller, service, service,
                        repository, configuration),
                List.of(endpoint, endpoint),
                List.of(entryPoint, entryPoint),
                List.of(dependency, dependency),
                AnalysisStatus.PARTIAL,
                List.of(info, firstWarning, repeatedWarning, secondWarning));

        AnalysisSummary summary = report.summary();

        assertEquals(1, summary.moduleCount());
        assertEquals(1, summary.sourceFileCount());
        assertEquals(5, summary.componentCount());
        assertEquals(2, summary.controllerCount());
        assertEquals(1, summary.serviceCount());
        assertEquals(1, summary.repositoryCount());
        assertEquals(1, summary.configurationCount());
        assertEquals(1, summary.endpointCount());
        assertEquals(1, summary.entryPointCount());
        assertEquals(1, summary.dependencyCount());
        assertEquals(AnalysisStatus.PARTIAL, summary.analysisStatus());
        assertTrue(summary.coverageLimited());
        assertEquals(List.of("A_WARNING", "Z_WARNING"), summary.coverageLimitationCodes());
    }

    @Test
    void successfulEmptyReportHasCompleteCoverage() {
        AnalysisReport report = new AnalysisReport(
                new Project("project:empty", "empty", BuildSystem.MAVEN, List.of()),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                AnalysisStatus.SUCCESS, List.of());

        AnalysisSummary summary = report.summary();

        assertFalse(summary.coverageLimited());
        assertEquals(List.of(), summary.coverageLimitationCodes());
    }

    @Test
    void rejectsNegativeCountsAndNormalizesLimitationCodes() {
        assertThrows(IllegalArgumentException.class, () -> new AnalysisSummary(
                -1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                AnalysisStatus.SUCCESS, List.of()));

        AnalysisSummary summary = new AnalysisSummary(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                AnalysisStatus.PARTIAL, List.of("Z", "A", "Z"));

        assertEquals(List.of("A", "Z"), summary.coverageLimitationCodes());
    }

    private static Component component(String id, ComponentKind kind) {
        return new Component(
                id,
                "module:root",
                "example." + id.substring(id.indexOf(':') + 1),
                Optional.empty(),
                kind,
                Framework.SPRING_BOOT,
                SOURCE,
                List.of(EVIDENCE));
    }

    private static Diagnostic diagnostic(String code, DiagnosticSeverity severity) {
        return new Diagnostic(
                code,
                severity,
                "TEST",
                Optional.empty(),
                Optional.empty(),
                Optional.of(POM),
                code);
    }
}
