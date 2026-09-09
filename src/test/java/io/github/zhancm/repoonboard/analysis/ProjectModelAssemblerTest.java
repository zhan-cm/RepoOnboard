package io.github.zhancm.repoonboard.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringDependencyKind;
import io.github.zhancm.repoonboard.analyzer.spring.SpringDependencyStatus;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointAnalysis;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringHttpMethod;
import io.github.zhancm.repoonboard.analyzer.spring.SpringMappingConditions;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.ComponentKind;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import io.github.zhancm.repoonboard.testing.FixturePaths;
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
        SpringComponentAnalysis components = new SpringComponentAnalyzer().analyze(parsed);
        var configuration = new SpringConfigurationAnalyzer().analyze(parsed);
        var controller = components.components().stream()
                .filter(component -> component.kind().name().equals("REST_CONTROLLER"))
                .findFirst().orElseThrow();
        var service = components.components().stream()
                .filter(component -> component.kind().name().equals("SERVICE"))
                .findFirst().orElseThrow();
        Evidence endpointEvidence = new Evidence(
                "SPRING_MAPPING", controller.location(), List.of(), "spring.mvc.mapping");
        SpringEndpointAnalysis endpoints = new SpringEndpointAnalysis(List.of(new SpringEndpointFact(
                "pom.xml", controller.qualifiedName(), "list", SpringHttpMethod.GET,
                Optional.of("/users"), false, SpringMappingConditions.empty(),
                controller.location(), List.of(endpointEvidence))), List.of());
        SpringComponentDependencyAnalysis dependencies = new SpringComponentDependencyAnalysis(
                List.of(new SpringComponentDependencyFact(
                        "pom.xml", controller.qualifiedName(), Optional.of("pom.xml"),
                        Optional.of(service.qualifiedName()), Optional.of(service.qualifiedName()),
                        service.qualifiedName(), SpringDependencyKind.COMPONENT_INJECTION,
                        SpringDependencyStatus.CONFIRMED, controller.location(),
                        List.of(new Evidence("SPRING_INJECTION", controller.location(), List.of(),
                                "spring.injection.constructor")))),
                List.of(new Diagnostic(
                        "FIXTURE_COVERAGE", DiagnosticSeverity.WARNING, "TEST",
                        Optional.of("module:pom.xml"), Optional.of(controller.location().sourceFileId()),
                        Optional.of(controller.location()), "Fixture demonstrates partial coverage")));

        var report = new ProjectModelAssembler().assemble(new ProjectAnalysisInput(
                maven, parsed, components, configuration, endpoints, dependencies));

        assertEquals("spring-analysis-project", report.project().name());
        assertEquals(1, report.modules().size());
        assertEquals(parsed.compilationUnits().size(), report.sourceFiles().size());
        assertTrue(report.components().stream().anyMatch(c -> c.kind() == ComponentKind.CONFIGURATION));
        assertEquals(1, report.entryPoints().size());
        assertEquals("/users", report.endpoints().getFirst().path().orElseThrow());
        assertTrue(report.dependencies().stream()
                .anyMatch(d -> d.kind() == DependencyKind.MAVEN_DECLARATION));
        assertTrue(report.dependencies().stream()
                .anyMatch(d -> d.kind() == DependencyKind.COMPONENT_INJECTION));
        assertEquals(AnalysisStatus.PARTIAL, report.status());
        assertFalse(report.diagnostics().isEmpty());
    }
}
