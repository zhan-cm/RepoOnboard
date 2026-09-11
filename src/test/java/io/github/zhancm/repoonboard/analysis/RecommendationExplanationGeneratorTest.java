package io.github.zhancm.repoonboard.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringInjectionAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringMvcMappingAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecommendationExplanationGeneratorTest {
    @TempDir Path temporaryDirectory;

    @Test
    void explainsEveryFixtureRecommendationWithResolvableEntitiesAndEvidence() {
        AnalysisReport report = assembleFixture();

        ExplainedReadingPath result = new RecommendationExplanationGenerator().generate(report);

        assertEquals(result.path().items().stream().map(RecommendedReadingItem::sourceFileId).toList(),
                result.explanations().stream()
                        .map(RecommendationExplanation::sourceFileId)
                        .toList());
        assertTrue(result.explanations().stream()
                .flatMap(explanation -> explanation.reasons().stream())
                .allMatch(reason -> !reason.evidence().isEmpty()));
        Set<String> entityIds = allEntityIds(report);
        assertTrue(result.explanations().stream()
                .flatMap(explanation -> explanation.reasons().stream())
                .flatMap(reason -> reason.supportingEntityIds().stream())
                .allMatch(entityIds::contains));

        RecommendationExplanation application = explanation(result, "Application.java");
        assertEquals(List.of(
                        RecommendationReasonKind.APPLICATION_ENTRY_POINT,
                        RecommendationReasonKind.CONFIGURATION_COMPONENT),
                application.reasons().stream().map(RecommendationReason::kind).toList());

        RecommendationReason controller = explanation(result, "OrderController.java").reasons().stream()
                .filter(reason -> reason.kind() == RecommendationReasonKind.HTTP_ENDPOINT_EXPOSURE)
                .findFirst()
                .orElseThrow();
        long endpointCount = report.endpoints().stream()
                .filter(endpoint -> endpoint.location().sourceFileId().endsWith("OrderController.java"))
                .count();
        assertEquals(endpointCount, controller.factCount());
        assertEquals("Exposes 2 HTTP endpoints", controller.message());

        List<RecommendationReason> serviceReasons = explanation(result, "OrderService.java").reasons();
        assertReason(serviceReasons, RecommendationReasonKind.CONFIRMED_DEPENDENCY_PATH, 1, 1);
        assertReason(serviceReasons, RecommendationReasonKind.CONFIRMED_INCOMING_DEPENDENCIES, 1, null);
        assertReason(serviceReasons, RecommendationReasonKind.CONFIRMED_OUTGOING_DEPENDENCIES, 1, null);
        assertReason(explanation(result, "OrderRepository.java").reasons(),
                RecommendationReasonKind.CONFIRMED_DEPENDENCY_PATH, 1, 2);
        assertFalse(result.coverageLimited());
        assertEquals(Optional.empty(), result.coverageNotice());
    }

    @Test
    void partialReportCarriesCoverageNoticeAndExactDiagnosticCodes() {
        AnalysisReport report = assembleFixture();
        Diagnostic diagnostic = new Diagnostic(
                "SPRING_MAPPING_PARTIAL",
                DiagnosticSeverity.WARNING,
                "SPRING_MVC_MAPPING",
                Optional.of(report.modules().getFirst().id()),
                Optional.of("src/main/java/fixture/start/OrderController.java"),
                Optional.of(SourceLocation.file(
                        "src/main/java/fixture/start/OrderController.java")),
                "A mapping could not be resolved.");
        AnalysisReport partial = new AnalysisReport(
                report.project(), report.modules(), report.sourceFiles(), report.components(),
                report.endpoints(), report.entryPoints(), report.dependencies(),
                AnalysisStatus.PARTIAL, List.of(diagnostic));

        ExplainedReadingPath result = new RecommendationExplanationGenerator().generate(partial);

        assertTrue(result.coverageLimited());
        assertEquals(AnalysisStatus.PARTIAL, result.analysisStatus());
        assertEquals(List.of("SPRING_MAPPING_PARTIAL"), result.coverageLimitationCodes());
        assertEquals(
                Optional.of("Recommendations are based on partial analysis and may omit important files."),
                result.coverageNotice());
    }

    @Test
    void explanationOutputIsStableWhenReportCollectionsAreReordered() {
        AnalysisReport report = assembleFixture();
        AnalysisReport reordered = new AnalysisReport(
                report.project(), reversed(report.modules()), reversed(report.sourceFiles()),
                reversed(report.components()), reversed(report.endpoints()),
                reversed(report.entryPoints()), reversed(report.dependencies()),
                report.status(), reversed(report.diagnostics()));

        RecommendationExplanationGenerator generator = new RecommendationExplanationGenerator();

        assertEquals(generator.generate(report), generator.generate(reordered));
    }

    @Test
    void rejectsUntraceableReasonsAndMismatchedCoverageContracts() {
        SourceLocation location = SourceLocation.file("src/main/java/example/Application.java");
        Evidence evidence = new Evidence("ENTRY", location, List.of(), "test.entry");
        RecommendationReason reason = new RecommendationReason(
                RecommendationReasonKind.APPLICATION_ENTRY_POINT,
                "Application entry point",
                1,
                Optional.empty(),
                List.of("entry:application"),
                List.of(evidence));
        ReadingImportanceAssessment assessment = new ReadingImportanceAssessment(
                location.sourceFileId(), Optional.of("module:root"), "entry:application",
                ReadingImportanceRule.APPLICATION_ENTRY_POINT,
                0, 0, 0, Optional.empty(), List.of());
        RecommendedReadingItem item = new RecommendedReadingItem(
                location.sourceFileId(), assessment.moduleId(), List.of(assessment));
        RecommendedReadingPath path = new RecommendedReadingPath(
                List.of(item), RecommendedReadingPath.DEFAULT_LIMIT);

        assertThrows(IllegalArgumentException.class, () -> new RecommendationReason(
                RecommendationReasonKind.APPLICATION_ENTRY_POINT,
                "Application entry point",
                1,
                Optional.empty(),
                List.of("entry:application"),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ExplainedReadingPath(
                path,
                List.of(new RecommendationExplanation("src/main/java/example/Other.java", List.of(reason))),
                AnalysisStatus.SUCCESS,
                List.of(),
                Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ExplainedReadingPath(
                path,
                List.of(new RecommendationExplanation(location.sourceFileId(), List.of(reason))),
                AnalysisStatus.PARTIAL,
                List.of("PARTIAL_TEST"),
                Optional.empty()));
    }

    private static RecommendationExplanation explanation(
            ExplainedReadingPath path,
            String fileSuffix) {
        return path.explanations().stream()
                .filter(explanation -> explanation.sourceFileId().endsWith(fileSuffix))
                .findFirst()
                .orElseThrow();
    }

    private static void assertReason(
            List<RecommendationReason> reasons,
            RecommendationReasonKind kind,
            int count,
            Integer distance) {
        RecommendationReason reason = reasons.stream()
                .filter(candidate -> candidate.kind() == kind)
                .findFirst()
                .orElseThrow();
        assertEquals(count, reason.factCount());
        assertEquals(Optional.ofNullable(distance), reason.dependencyDistance());
    }

    private static Set<String> allEntityIds(AnalysisReport report) {
        Set<String> ids = new HashSet<>();
        report.modules().forEach(value -> ids.add(value.id()));
        report.components().forEach(value -> ids.add(value.id()));
        report.endpoints().forEach(value -> ids.add(value.id()));
        report.entryPoints().forEach(value -> ids.add(value.id()));
        report.dependencies().forEach(value -> ids.add(value.id()));
        return Set.copyOf(ids);
    }

    private AnalysisReport assembleFixture() {
        Path root = FixturePaths.project("start-here-project");
        var maven = new MavenModuleAnalyzer().analyze(
                root, new MavenModelOptions(temporaryDirectory.resolve("cache"), List.of(), 1_048_576));
        var roots = new JavaSourceRootDiscoverer().discover(root, maven);
        var files = new JavaFileDiscoverer().discover(root, roots);
        var java = new JavaTypeReferenceResolver().resolve(
                new JavaSourceParser().parse(root, files), maven);
        var components = new SpringComponentAnalyzer().analyze(java);
        var configuration = new SpringConfigurationAnalyzer().analyze(java);
        var injection = new SpringInjectionAnalyzer().analyze(java, components, configuration);
        var mappings = new SpringMvcMappingAnalyzer().analyze(java, components);
        var endpoints = new SpringEndpointAnalyzer().analyze(mappings);
        var dependencies = new SpringComponentDependencyAnalyzer().analyze(components, injection);
        return new ProjectModelAssembler().assemble(new ProjectAnalysisInput(
                maven, java, components, configuration, endpoints, dependencies));
    }

    private static <T> List<T> reversed(List<T> values) {
        List<T> result = new ArrayList<>(values);
        Collections.reverse(result);
        return List.copyOf(result);
    }
}
