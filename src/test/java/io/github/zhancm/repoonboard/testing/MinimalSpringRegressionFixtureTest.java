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
import io.github.zhancm.repoonboard.core.model.Endpoint;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MinimalSpringRegressionFixtureTest {

    private static final String MODULE_ID = "module:pom.xml";
    private static final String CONTROLLER_ID =
            "component:module%3Apom.xml:fixture.web.UserController";
    private static final String SERVICE_ID =
            "component:module%3Apom.xml:fixture.service.UserService";
    private static final String REPOSITORY_ID =
            "component:module%3Apom.xml:fixture.repository.UserRepository";

    @TempDir
    Path temporaryDirectory;

    @Test
    void producesStableTraceableOutputWithoutBuildingOrLaunchingTheFixture() throws IOException {
        Path fixture = FixturePaths.project("spring-analysis-project");
        Set<String> filesBefore = relativeFiles(fixture);
        Path emptyLocalRepository = temporaryDirectory.resolve("empty-local-repository");

        AnalysisReport report = analyze(fixture, emptyLocalRepository);
        AnalysisReport repeated = analyze(fixture, emptyLocalRepository);

        assertEquals(report, repeated);
        assertEquals(AnalysisStatus.SUCCESS, report.status());
        assertTrue(report.diagnostics().isEmpty());
        assertEquals("project:pom.xml", report.project().id());
        assertEquals(MODULE_ID, report.modules().getFirst().id());

        assertComponent(
                component(report, "fixture.web.UserController"),
                CONTROLLER_ID,
                ComponentKind.REST_CONTROLLER,
                "src/main/java/fixture/web/UserController.java",
                8,
                "spring.component.direct_annotation:org.springframework.web.bind.annotation.RestController");
        assertComponent(
                component(report, "fixture.service.UserService"),
                SERVICE_ID,
                ComponentKind.SERVICE,
                "src/main/java/fixture/service/UserService.java",
                6,
                "spring.component.direct_annotation:org.springframework.stereotype.Service");
        assertComponent(
                component(report, "fixture.repository.UserRepository"),
                REPOSITORY_ID,
                ComponentKind.REPOSITORY,
                "src/main/java/fixture/repository/UserRepository.java",
                5,
                "spring.component.direct_annotation:org.springframework.stereotype.Repository");

        assertEquals(1, report.endpoints().size());
        Endpoint endpoint = report.endpoints().getFirst();
        assertEquals("endpoint:58697ed87c5e8c32383ecafe", endpoint.id());
        assertEquals(MODULE_ID, endpoint.moduleId());
        assertEquals(CONTROLLER_ID, endpoint.componentId());
        assertEquals("GET", endpoint.httpMethod());
        assertEquals("/users/{id}", endpoint.path().orElseThrow());
        assertFalse(endpoint.unresolvedPath());
        assertEquals("findById", endpoint.handlerMethod());
        assertEquals("src/main/java/fixture/web/UserController.java",
                endpoint.location().sourceFileId());
        assertEquals(17, endpoint.location().startLine().orElseThrow());
        assertEquals(List.of("SPRING_MVC_METHOD_MAPPING", "SPRING_MVC_TYPE_MAPPING"),
                endpoint.evidence().stream().map(Evidence::type).toList());
        assertEquals(List.of(17, 9), endpoint.evidence().stream()
                .map(evidence -> evidence.location().startLine().orElseThrow())
                .toList());
        assertEquals(List.of(
                        "spring.mvc.mapping.annotation:org.springframework.web.bind.annotation.GetMapping",
                        "spring.mvc.mapping.annotation:org.springframework.web.bind.annotation.RequestMapping"),
                endpoint.evidence().stream().map(Evidence::ruleId).toList());

        assertDependency(
                report,
                "dependency:d80399e4494ab91e169ca28a",
                CONTROLLER_ID,
                SERVICE_ID,
                "UserService",
                "src/main/java/fixture/web/UserController.java",
                13);
        assertDependency(
                report,
                "dependency:ac227220b061af19a8dc2887",
                SERVICE_ID,
                REPOSITORY_ID,
                "UserRepository",
                "src/main/java/fixture/service/UserService.java",
                10);

        assertEquals(filesBefore, relativeFiles(fixture));
        assertFalse(Files.exists(fixture.resolve("target")));
        assertFalse(Files.exists(emptyLocalRepository));
    }

    private static void assertComponent(
            Component component,
            String expectedId,
            ComponentKind expectedKind,
            String expectedSource,
            int expectedLine,
            String expectedRuleId) {
        assertEquals(expectedId, component.id());
        assertEquals(MODULE_ID, component.moduleId());
        assertEquals(expectedKind, component.kind());
        assertEquals(expectedSource, component.location().sourceFileId());
        assertEquals(expectedLine, component.location().startLine().orElseThrow());
        assertEquals(1, component.evidence().size());
        Evidence evidence = component.evidence().getFirst();
        assertEquals("SPRING_COMPONENT_ANNOTATION", evidence.type());
        assertEquals(expectedSource, evidence.location().sourceFileId());
        assertEquals(expectedLine, evidence.location().startLine().orElseThrow());
        assertEquals(expectedRuleId, evidence.ruleId());
        assertTrue(evidence.relatedLocations().contains(component.location()));
    }

    private static void assertDependency(
            AnalysisReport report,
            String expectedId,
            String sourceId,
            String targetId,
            String declaredTarget,
            String expectedSource,
            int expectedLine) {
        Dependency dependency = report.dependencies().stream()
                .filter(candidate -> candidate.id().equals(expectedId))
                .findFirst()
                .orElseThrow();
        assertEquals(sourceId, dependency.sourceId());
        assertEquals(targetId, dependency.targetId().orElseThrow());
        assertEquals(declaredTarget, dependency.declaredTarget());
        assertEquals(DependencyKind.COMPONENT_INJECTION, dependency.kind());
        assertEquals(ResolutionStatus.CONFIRMED, dependency.status());
        assertEquals(expectedSource, dependency.location().sourceFileId());
        assertEquals(expectedLine, dependency.location().startLine().orElseThrow());
        assertEquals(1, dependency.evidence().size());
        assertEquals("CONSTRUCTOR_INJECTION", dependency.evidence().getFirst().type());
        assertEquals("spring.injection.single_constructor",
                dependency.evidence().getFirst().ruleId());
    }

    private static Component component(AnalysisReport report, String qualifiedName) {
        return report.components().stream()
                .filter(candidate -> candidate.qualifiedName().equals(qualifiedName))
                .findFirst()
                .orElseThrow();
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
