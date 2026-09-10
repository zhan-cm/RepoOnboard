package io.github.zhancm.repoonboard.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalysis;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentAnalysis;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyAnalysis;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringDependencyKind;
import io.github.zhancm.repoonboard.analyzer.spring.SpringDependencyStatus;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointAnalysis;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringHttpMethod;
import io.github.zhancm.repoonboard.analyzer.spring.SpringMappingConditions;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringInjectionAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringMvcMappingAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.ComponentKind;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.StableIdentifiers;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectModelAssemblerTest {
    @TempDir Path temporaryDirectory;

    @Test
    void mapsMavenJavaAndSpringFactsWithoutLeakingAnalyzerTypes() {
        Path root = FixturePaths.project("spring-analysis-project");
        MavenModuleAnalysis maven = new MavenModuleAnalyzer().analyze(
                root, new MavenModelOptions(temporaryDirectory.resolve("cache"), List.of(), 1_048_576));
        var roots = new JavaSourceRootDiscoverer().discover(root, maven);
        var files = new JavaFileDiscoverer().discover(root, roots);
        JavaParseAnalysis parsed = new JavaTypeReferenceResolver().resolve(
                new JavaSourceParser().parse(root, files), maven);
        SpringComponentAnalysis detectedComponents = new SpringComponentAnalyzer().analyze(parsed);
        var configuration = new SpringConfigurationAnalyzer().analyze(parsed);
        var controller = detectedComponents.components().stream()
                .filter(component -> component.kind().name().equals("REST_CONTROLLER"))
                .findFirst().orElseThrow();
        var service = detectedComponents.components().stream()
                .filter(component -> component.kind().name().equals("SERVICE"))
                .findFirst().orElseThrow();
        var repository = detectedComponents.components().stream()
                .filter(component -> component.kind().name().equals("REPOSITORY"))
                .findFirst().orElseThrow();
        SpringComponentAnalysis components = new SpringComponentAnalysis(
                java.util.stream.Stream.concat(
                        detectedComponents.components().stream(), java.util.stream.Stream.of(controller))
                        .toList(),
                detectedComponents.diagnostics());
        Evidence endpointEvidence = new Evidence(
                "SPRING_MAPPING", controller.location(), List.of(), "spring.mvc.mapping");
        SpringEndpointFact getEndpoint = new SpringEndpointFact(
                "pom.xml", controller.qualifiedName(), "list", SpringHttpMethod.GET,
                Optional.of("/users"), false, SpringMappingConditions.empty(),
                controller.location(), List.of(endpointEvidence));
        SpringEndpointFact postEndpoint = new SpringEndpointFact(
                "pom.xml", controller.qualifiedName(), "create", SpringHttpMethod.POST,
                Optional.of("/users"), false, SpringMappingConditions.empty(),
                controller.location(), List.of(endpointEvidence));
        SpringEndpointAnalysis endpoints = new SpringEndpointAnalysis(
                List.of(postEndpoint, getEndpoint, getEndpoint), List.of());
        SpringComponentDependencyFact serviceDependency = dependency(
                controller, service, "spring.injection.constructor");
        SpringComponentDependencyFact repositoryDependency = dependency(
                controller, repository, "spring.injection.field");
        SpringComponentDependencyAnalysis dependencies = new SpringComponentDependencyAnalysis(
                List.of(repositoryDependency, serviceDependency, repositoryDependency), List.of());

        var report = new ProjectModelAssembler().assemble(new ProjectAnalysisInput(
                maven, parsed, components, configuration, endpoints, dependencies));

        assertEquals("spring-analysis-project", report.project().name());
        assertEquals(1, report.modules().size());
        assertEquals(parsed.compilationUnits().size(), report.sourceFiles().size());
        assertTrue(report.components().stream().anyMatch(c -> c.kind() == ComponentKind.CONFIGURATION));
        assertEquals(1, report.entryPoints().size());
        assertEquals(3, report.endpoints().size());
        assertTrue(report.endpoints().stream()
                .allMatch(endpoint -> endpoint.path().orElseThrow().equals("/users")));
        assertTrue(report.dependencies().stream()
                .anyMatch(d -> d.kind() == DependencyKind.MAVEN_DECLARATION));
        assertTrue(report.dependencies().stream()
                .anyMatch(d -> d.kind() == DependencyKind.COMPONENT_INJECTION));
        assertEquals("3.5.0", report.modules().getFirst().frameworkVersions().getFirst().version());
        assertEquals(AnalysisStatus.PARTIAL, report.status());
        assertTrue(report.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("REPORT_ID_COLLISION")));
        assertEquals(report.components().size(), report.components().stream()
                .map(component -> component.id()).distinct().count());
        assertEquals(report.endpoints().size(), report.endpoints().stream()
                .map(endpoint -> endpoint.id()).distinct().count());
        assertEquals(report.dependencies().size(), report.dependencies().stream()
                .map(dependency -> dependency.id()).distinct().count());
        assertTrue(report.diagnostics().stream()
                .map(diagnostic -> diagnostic.message())
                .anyMatch(message -> message.startsWith("ENDPOINT key occurs")));
        assertTrue(report.diagnostics().stream()
                .map(diagnostic -> diagnostic.message())
                .anyMatch(message -> message.startsWith("DEPENDENCY key occurs")));

        SpringComponentAnalysis reorderedComponents = new SpringComponentAnalysis(
                reversed(components.components()), components.diagnostics());
        SpringEndpointAnalysis reorderedEndpoints = new SpringEndpointAnalysis(
                reversed(endpoints.endpoints()), endpoints.diagnostics());
        SpringComponentDependencyAnalysis reorderedDependencies =
                new SpringComponentDependencyAnalysis(
                        reversed(dependencies.dependencies()), dependencies.diagnostics());
        var reorderedReport = new ProjectModelAssembler().assemble(new ProjectAnalysisInput(
                maven, parsed, reorderedComponents, configuration,
                reorderedEndpoints, reorderedDependencies));
        assertEquals(report, reorderedReport);

        var publicController = report.components().stream()
                .filter(component -> component.qualifiedName().equals(controller.qualifiedName()))
                .findFirst().orElseThrow();
        assertNotEquals(publicController.id(), StableIdentifiers.component(
                publicController.moduleId(), "fixture.web.AccountController"));
        var publicGet = report.endpoints().stream()
                .filter(endpoint -> endpoint.httpMethod().equals("GET"))
                .findFirst().orElseThrow();
        assertNotEquals(publicGet.id(), StableIdentifiers.endpoint(
                publicGet.componentId(), "findAll", publicGet.httpMethod(), publicGet.path(),
                publicGet.unresolvedPath(), publicGet.conditions()));
    }

    @Test
    void exposesAggregationHierarchyAndConfirmedInternalModuleDependencies() {
        var report = assembleFixture("multi-module-project");
        var root = report.modules().stream()
                .filter(module -> module.pomFileId().equals("pom.xml"))
                .findFirst().orElseThrow();
        var api = report.modules().stream()
                .filter(module -> module.pomFileId().equals("api/pom.xml"))
                .findFirst().orElseThrow();
        var library = report.modules().stream()
                .filter(module -> module.pomFileId().equals("library/pom.xml"))
                .findFirst().orElseThrow();

        assertEquals(Optional.empty(), root.aggregationParentModuleId());
        assertEquals(Optional.of(root.id()), api.aggregationParentModuleId());
        assertEquals(Optional.of(root.id()), library.aggregationParentModuleId());
        assertTrue(api.evidence().stream()
                .anyMatch(evidence -> evidence.type().equals("MAVEN_MODULE_AGGREGATION")));
        assertTrue(report.dependencies().stream().anyMatch(dependency ->
                dependency.kind() == DependencyKind.MAVEN_DECLARATION
                        && dependency.sourceId().equals(api.id())
                        && dependency.targetId().equals(Optional.of(library.id()))));
    }

    @Test
    void exposesJavaVersionOnlyFromResolvedMavenMetadata() {
        var report = assembleFixture("minimal-maven-project");

        assertEquals(1, report.modules().getFirst().languageVersions().size());
        assertEquals("21", report.modules().getFirst().languageVersions().getFirst().version());
        assertTrue(report.modules().getFirst().languageVersions().getFirst().evidence().stream()
                .anyMatch(evidence -> evidence.type().equals("MAVEN_LANGUAGE_VERSION")));
    }

    private io.github.zhancm.repoonboard.core.model.AnalysisReport assembleFixture(String fixtureName) {
        Path root = FixturePaths.project(fixtureName);
        MavenModuleAnalysis maven = new MavenModuleAnalyzer().analyze(
                root, new MavenModelOptions(temporaryDirectory.resolve(fixtureName), List.of(), 1_048_576));
        var roots = new JavaSourceRootDiscoverer().discover(root, maven);
        var files = new JavaFileDiscoverer().discover(root, roots);
        JavaParseAnalysis java = new JavaTypeReferenceResolver().resolve(
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

    private static SpringComponentDependencyFact dependency(
            io.github.zhancm.repoonboard.analyzer.spring.SpringComponentFact source,
            io.github.zhancm.repoonboard.analyzer.spring.SpringComponentFact target,
            String ruleId) {
        return new SpringComponentDependencyFact(
                "pom.xml", source.qualifiedName(), Optional.of("pom.xml"),
                Optional.of(target.qualifiedName()), Optional.of(target.qualifiedName()),
                target.qualifiedName(), SpringDependencyKind.COMPONENT_INJECTION,
                SpringDependencyStatus.CONFIRMED, source.location(),
                List.of(new Evidence("SPRING_INJECTION", source.location(), List.of(), ruleId)));
    }

    private static <T> List<T> reversed(List<T> values) {
        List<T> result = new ArrayList<>(values);
        Collections.reverse(result);
        return List.copyOf(result);
    }
}
