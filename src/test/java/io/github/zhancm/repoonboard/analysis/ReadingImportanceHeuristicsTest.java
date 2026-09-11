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
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadingImportanceHeuristicsTest {
    @TempDir Path temporaryDirectory;

    @Test
    void prioritizesVerifiedFixtureFactsAndConfirmedDependencyDistance() {
        AnalysisReport report = assembleFixture();

        List<ReadingImportanceAssessment> result = new ReadingImportanceHeuristics().evaluate(report);

        assertEquals(List.of(
                        ReadingImportanceRule.ROOT_POM,
                        ReadingImportanceRule.APPLICATION_ENTRY_POINT,
                        ReadingImportanceRule.CONFIGURATION,
                        ReadingImportanceRule.CONFIGURATION,
                        ReadingImportanceRule.API_CONTROLLER,
                        ReadingImportanceRule.API_CONTROLLER,
                        ReadingImportanceRule.CONFIRMED_DEPENDENCY_NEIGHBOR,
                        ReadingImportanceRule.CONFIRMED_DEPENDENCY_NEIGHBOR),
                result.stream().map(ReadingImportanceAssessment::rule).toList());
        assertEquals("pom.xml", result.get(0).sourceFileId());
        assertEquals("src/main/java/fixture/start/Application.java", result.get(1).sourceFileId());

        List<ReadingImportanceAssessment> controllers = result.stream()
                .filter(value -> value.rule() == ReadingImportanceRule.API_CONTROLLER)
                .toList();
        assertEquals(List.of(2, 1), controllers.stream()
                .map(ReadingImportanceAssessment::endpointCount)
                .toList());
        assertTrue(controllers.get(0).sourceFileId().endsWith("OrderController.java"));

        List<ReadingImportanceAssessment> neighbors = result.stream()
                .filter(value -> value.rule() == ReadingImportanceRule.CONFIRMED_DEPENDENCY_NEIGHBOR)
                .toList();
        assertEquals(List.of(1, 2), neighbors.stream()
                .map(value -> value.dependencyDistance().orElseThrow())
                .toList());
        assertTrue(neighbors.get(0).sourceFileId().endsWith("OrderService.java"));
        assertTrue(neighbors.get(1).sourceFileId().endsWith("OrderRepository.java"));
        assertFalse(neighbors.stream()
                .anyMatch(value -> value.sourceFileId().endsWith("CriticalBusinessService.java")));
        assertTrue(neighbors.stream().allMatch(value -> !value.supportingDependencyIds().isEmpty()));
    }

    @Test
    void producesTheSameSignalsForReorderedReportCollections() {
        AnalysisReport report = assembleFixture();
        AnalysisReport reordered = new AnalysisReport(
                report.project(), reversed(report.modules()), reversed(report.sourceFiles()),
                reversed(report.components()), reversed(report.endpoints()),
                reversed(report.entryPoints()), reversed(report.dependencies()),
                report.status(), reversed(report.diagnostics()));

        ReadingImportanceHeuristics heuristics = new ReadingImportanceHeuristics();

        assertEquals(heuristics.evaluate(report), heuristics.evaluate(reordered));
    }

    @Test
    void ignoresUnconfirmedAndNonComponentRelationships() {
        AnalysisReport report = assembleFixture();
        var detached = report.components().stream()
                .filter(component -> component.qualifiedName().endsWith("CriticalBusinessService"))
                .findFirst()
                .orElseThrow();
        var controller = report.components().stream()
                .filter(component -> component.qualifiedName().endsWith("StatusController"))
                .findFirst()
                .orElseThrow();
        Dependency unresolved = new Dependency(
                "dependency:unresolved-test", controller.id(), Optional.of(detached.id()),
                detached.qualifiedName(), DependencyKind.COMPONENT_INJECTION,
                ResolutionStatus.UNRESOLVED, controller.location(), controller.evidence());
        Dependency maven = new Dependency(
                "dependency:maven-test", controller.moduleId(), Optional.of(detached.id()),
                detached.qualifiedName(), DependencyKind.MAVEN_DECLARATION,
                ResolutionStatus.CONFIRMED, controller.location(), controller.evidence());
        List<Dependency> dependencies = new ArrayList<>(report.dependencies());
        dependencies.add(unresolved);
        dependencies.add(maven);
        AnalysisReport augmented = new AnalysisReport(
                report.project(), report.modules(), report.sourceFiles(), report.components(),
                report.endpoints(), report.entryPoints(), dependencies, report.status(), report.diagnostics());

        List<ReadingImportanceAssessment> result = new ReadingImportanceHeuristics().evaluate(augmented);

        assertFalse(result.stream().anyMatch(value -> value.subjectId().equals(detached.id())));
    }

    @Test
    void validatesDependencyNeighborSupport() {
        assertThrows(IllegalArgumentException.class, () -> new ReadingImportanceAssessment(
                "src/main/java/example/Service.java", Optional.of("module:root"), "component:service",
                ReadingImportanceRule.CONFIRMED_DEPENDENCY_NEIGHBOR,
                0, 1, 0, Optional.empty(), List.of("dependency:controller-service")));
        assertThrows(IllegalArgumentException.class, () -> new ReadingImportanceAssessment(
                "src/main/java/example/Service.java", Optional.of("module:root"), "component:service",
                ReadingImportanceRule.CONFIRMED_DEPENDENCY_NEIGHBOR,
                0, 1, 0, Optional.of(1), List.of()));
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
