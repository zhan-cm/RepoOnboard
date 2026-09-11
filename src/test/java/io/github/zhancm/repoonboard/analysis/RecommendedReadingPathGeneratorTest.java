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
import io.github.zhancm.repoonboard.core.model.BuildSystem;
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.EntryPoint;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.Framework;
import io.github.zhancm.repoonboard.core.model.Module;
import io.github.zhancm.repoonboard.core.model.Project;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecommendedReadingPathGeneratorTest {
    @TempDir Path temporaryDirectory;

    @Test
    void deduplicatesFilesWhileRetainingTheirOrderedImportanceSignals() {
        RecommendedReadingPath path = new RecommendedReadingPathGenerator().generate(assembleFixture());

        assertEquals(List.of(
                        "pom.xml",
                        "src/main/java/fixture/start/Application.java",
                        "src/main/java/fixture/start/CoreConfiguration.java",
                        "src/main/java/fixture/start/OrderController.java",
                        "src/main/java/fixture/start/StatusController.java",
                        "src/main/java/fixture/start/OrderService.java",
                        "src/main/java/fixture/start/OrderRepository.java"),
                path.items().stream().map(RecommendedReadingItem::sourceFileId).toList());
        assertEquals(List.of(
                        ReadingImportanceRule.APPLICATION_ENTRY_POINT,
                        ReadingImportanceRule.CONFIGURATION),
                path.items().get(1).assessments().stream()
                        .map(ReadingImportanceAssessment::rule)
                        .toList());
        assertEquals(ReadingImportanceRule.APPLICATION_ENTRY_POINT,
                path.items().get(1).primaryAssessment().rule());
        assertFalse(path.expandable());
        assertEquals(path.items(), path.defaultItems());
    }

    @Test
    void limitsTheDefaultViewToTenAndRetainsTheExpandableCompletePath() {
        List<RecommendedReadingItem> items = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            String sourceFileId = "src/main/java/example/Entry" + index + ".java";
            ReadingImportanceAssessment assessment = assessment(sourceFileId, "entry:" + index);
            items.add(new RecommendedReadingItem(
                    sourceFileId, Optional.of("module:root"), List.of(assessment)));
        }
        RecommendedReadingPath path = new RecommendedReadingPath(
                items, RecommendedReadingPath.DEFAULT_LIMIT);

        assertEquals(10, path.defaultItems().size());
        assertEquals(12, path.visibleItems(true).size());
        assertEquals(path.defaultItems(), path.visibleItems(false));
        assertTrue(path.expandable());
        assertEquals(2, path.hiddenItemCount());
        assertThrows(UnsupportedOperationException.class, () -> path.items().clear());
    }

    @Test
    void remainsStableWhenReportCollectionsArriveInReverseOrder() {
        AnalysisReport report = assembleFixture();
        AnalysisReport reordered = new AnalysisReport(
                report.project(), reversed(report.modules()), reversed(report.sourceFiles()),
                reversed(report.components()), reversed(report.endpoints()),
                reversed(report.entryPoints()), reversed(report.dependencies()),
                report.status(), reversed(report.diagnostics()));

        RecommendedReadingPathGenerator generator = new RecommendedReadingPathGenerator();

        assertEquals(generator.generate(report), generator.generate(reordered));
    }

    @Test
    void ordersFilesFromDifferentModulesByStableModuleIdentity() {
        Module root = module("module:root", "pom.xml", ".", Optional.empty());
        Module beta = module("module:beta", "beta/pom.xml", "beta", Optional.of(root.id()));
        Module alpha = module("module:alpha", "alpha/pom.xml", "alpha", Optional.of(root.id()));
        EntryPoint betaEntry = entryPoint(
                "entry:beta", beta.id(), "beta.Application", "beta/src/main/java/Application.java");
        EntryPoint alphaEntry = entryPoint(
                "entry:alpha", alpha.id(), "alpha.Application", "alpha/src/main/java/Application.java");
        AnalysisReport report = new AnalysisReport(
                new Project("project:multi", "multi", BuildSystem.MAVEN, List.of()),
                List.of(beta, root, alpha), List.of(), List.of(), List.of(),
                List.of(betaEntry, alphaEntry), List.of(), AnalysisStatus.SUCCESS, List.of());

        RecommendedReadingPath path = new RecommendedReadingPathGenerator().generate(report);

        assertEquals(List.of("module:root", "module:alpha", "module:beta"), path.items().stream()
                .map(item -> item.moduleId().orElseThrow())
                .toList());
    }

    @Test
    void unresolvedDependencyDoesNotAddAFileToThePath() {
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
                "dependency:unresolved-path-test", controller.id(), Optional.of(detached.id()),
                detached.qualifiedName(), DependencyKind.COMPONENT_INJECTION,
                ResolutionStatus.UNRESOLVED, controller.location(), controller.evidence());
        List<Dependency> dependencies = new ArrayList<>(report.dependencies());
        dependencies.add(unresolved);
        AnalysisReport augmented = new AnalysisReport(
                report.project(), report.modules(), report.sourceFiles(), report.components(),
                report.endpoints(), report.entryPoints(), dependencies, report.status(), report.diagnostics());

        RecommendedReadingPath path = new RecommendedReadingPathGenerator().generate(augmented);

        assertFalse(path.items().stream()
                .anyMatch(item -> item.sourceFileId().endsWith("CriticalBusinessService.java")));
    }

    @Test
    void rejectsDuplicateFilesAndInconsistentItemOwnership() {
        ReadingImportanceAssessment assessment = assessment(
                "src/main/java/example/Application.java", "entry:application");
        RecommendedReadingItem item = new RecommendedReadingItem(
                assessment.sourceFileId(), assessment.moduleId(), List.of(assessment));

        assertThrows(IllegalArgumentException.class, () -> new RecommendedReadingPath(
                List.of(item, item), RecommendedReadingPath.DEFAULT_LIMIT));
        assertThrows(IllegalArgumentException.class, () -> new RecommendedReadingPath(List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> new RecommendedReadingPath(List.of(), 11));
        assertThrows(IllegalArgumentException.class, () -> new RecommendedReadingItem(
                "src/main/java/example/Other.java", assessment.moduleId(), List.of(assessment)));
        assertThrows(IllegalArgumentException.class, () -> new RecommendedReadingItem(
                assessment.sourceFileId(), Optional.of("module:other"), List.of(assessment)));
    }

    private static ReadingImportanceAssessment assessment(String sourceFileId, String subjectId) {
        return new ReadingImportanceAssessment(
                sourceFileId, Optional.of("module:root"), subjectId,
                ReadingImportanceRule.APPLICATION_ENTRY_POINT,
                0, 0, 0, Optional.empty(), List.of());
    }

    private static Module module(
            String id,
            String pomFileId,
            String baseDirectory,
            Optional<String> parentId) {
        return new Module(
                id, pomFileId, baseDirectory, parentId,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static EntryPoint entryPoint(
            String id,
            String moduleId,
            String qualifiedName,
            String sourceFileId) {
        SourceLocation location = SourceLocation.file(sourceFileId);
        Evidence evidence = new Evidence("APPLICATION_ENTRY", location, List.of(), "test.entry");
        return new EntryPoint(
                id, moduleId, qualifiedName, "APPLICATION", Framework.SPRING_BOOT,
                location, List.of(evidence));
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
