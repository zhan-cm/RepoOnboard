package io.github.zhancm.repoonboard.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analysis.ProjectAnalysisInput;
import io.github.zhancm.repoonboard.analysis.ProjectModelAssembler;
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
import io.github.zhancm.repoonboard.core.model.Component;
import io.github.zhancm.repoonboard.core.model.ComponentKind;
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringAccuracyRegressionFixtureTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void closesRepositoryAndWildcardGapsWithoutExecutingTheFixture() throws IOException {
        Path fixture = FixturePaths.project("spring-accuracy-project");
        Set<String> filesBefore = relativeFiles(fixture);
        Path emptyLocalRepository = temporaryDirectory.resolve("empty-local-repository");

        AnalysisReport report = analyze(fixture, emptyLocalRepository);
        AnalysisReport repeated = analyze(fixture, emptyLocalRepository);

        assertEquals(report, repeated);
        assertEquals(AnalysisStatus.SUCCESS, report.status());
        assertTrue(report.diagnostics().isEmpty());
        assertEquals(3, report.components().size());
        assertEquals(1, report.endpoints().size());
        List<Dependency> componentDependencies = report.dependencies().stream()
                .filter(dependency -> dependency.kind() == DependencyKind.COMPONENT_INJECTION)
                .toList();
        assertEquals(3, componentDependencies.size());
        assertTrue(componentDependencies.stream()
                .allMatch(dependency -> dependency.status() == ResolutionStatus.CONFIRMED));

        Map<String, Component> components = report.components().stream()
                .collect(Collectors.toMap(Component::qualifiedName, Function.identity()));
        Component repository = components.get("fixture.accuracy.repository.OwnerRepository");
        assertEquals(ComponentKind.REPOSITORY, repository.kind());
        assertEquals(
                "component:module%3Apom.xml:fixture.accuracy.repository.OwnerRepository",
                repository.id());
        assertEquals("src/main/java/fixture/accuracy/repository/OwnerRepository.java",
                repository.location().sourceFileId());
        assertEquals(6, repository.location().startLine().orElseThrow());
        Evidence repositoryEvidence = repository.evidence().getFirst();
        assertEquals("SPRING_DATA_REPOSITORY_INHERITANCE", repositoryEvidence.type());
        assertEquals(6, repositoryEvidence.location().startLine().orElseThrow());
        assertEquals(
                "spring.component.repository_inheritance:org.springframework.data.repository.Repository",
                repositoryEvidence.ruleId());

        Component controller = components.get("fixture.accuracy.web.OwnerController");
        assertEquals(ComponentKind.REST_CONTROLLER, controller.kind());
        assertEquals(
                "spring.component.direct_annotation:org.springframework.web.bind.annotation.RestController",
                controller.evidence().getFirst().ruleId());
        assertFalse(report.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_COMPONENT_ANNOTATION_AMBIGUOUS")));

        assertEquals("GET", report.endpoints().getFirst().httpMethod());
        assertEquals("/owners", report.endpoints().getFirst().path().orElseThrow());
        assertEquals("list", report.endpoints().getFirst().handlerMethod());

        Set<String> edges = componentDependencies.stream()
                .map(dependency -> sourceName(dependency, report) + "->" + targetName(dependency, report))
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "OwnerController->OwnerRepository",
                "OwnerController->OwnerService",
                "OwnerService->OwnerRepository"), edges);

        assertEquals(filesBefore, relativeFiles(fixture));
        assertFalse(Files.exists(fixture.resolve("target")));
        assertFalse(Files.exists(emptyLocalRepository));
    }

    private static String sourceName(Dependency dependency, AnalysisReport report) {
        return report.components().stream()
                .filter(component -> component.id().equals(dependency.sourceId()))
                .map(SpringAccuracyRegressionFixtureTest::simpleName)
                .findFirst()
                .orElseThrow();
    }

    private static String targetName(Dependency dependency, AnalysisReport report) {
        String targetId = dependency.targetId().orElseThrow();
        return report.components().stream()
                .filter(component -> component.id().equals(targetId))
                .map(SpringAccuracyRegressionFixtureTest::simpleName)
                .findFirst()
                .orElseThrow();
    }

    private static String simpleName(Component component) {
        String qualifiedName = component.qualifiedName();
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    private static AnalysisReport analyze(Path fixture, Path localRepository) {
        var maven = new MavenModuleAnalyzer().analyze(
                fixture, new MavenModelOptions(localRepository, List.of(), 1_048_576));
        var roots = new JavaSourceRootDiscoverer().discover(fixture, maven);
        var files = new JavaFileDiscoverer().discover(fixture, roots);
        var java = new JavaTypeReferenceResolver().resolve(
                new JavaSourceParser().parse(fixture, files), maven);
        var components = new SpringComponentAnalyzer().analyze(java);
        var configuration = new SpringConfigurationAnalyzer().analyze(java);
        var injection = new SpringInjectionAnalyzer().analyze(java, components, configuration);
        var mappings = new SpringMvcMappingAnalyzer().analyze(java, components);
        var endpoints = new SpringEndpointAnalyzer().analyze(mappings);
        var dependencies = new SpringComponentDependencyAnalyzer().analyze(components, injection);
        return new ProjectModelAssembler().assemble(new ProjectAnalysisInput(
                maven, java, components, configuration, endpoints, dependencies));
    }

    private static Set<String> relativeFiles(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
