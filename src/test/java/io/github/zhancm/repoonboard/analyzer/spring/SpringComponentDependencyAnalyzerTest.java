package io.github.zhancm.repoonboard.analyzer.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analyzer.java.JavaDeclarationIndex;
import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringComponentDependencyAnalyzerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void buildsConfirmedDirectedComponentEdgesWithInjectionEvidence() {
        Path fixture = FixturePaths.project("spring-dependency-project");
        Pipeline pipeline = analyze(fixture, temporaryDirectory.resolve("cache"));

        SpringComponentDependencyAnalysis analysis = new SpringComponentDependencyAnalyzer().analyze(
                pipeline.components(), pipeline.injections());

        assertEquals(4, analysis.dependencies().size());
        assertTrue(analysis.dependencies().stream()
                .allMatch(dependency -> dependency.status() == SpringDependencyStatus.CONFIRMED));
        assertTrue(analysis.dependencies().stream()
                .allMatch(dependency -> dependency.kind() == SpringDependencyKind.COMPONENT_INJECTION));
        Set<String> edges = analysis.dependencies().stream()
                .map(dependency -> simple(dependency.sourceQualifiedName()) + "->"
                        + simple(dependency.targetQualifiedName().orElseThrow()))
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "OrderController->OrderService",
                "OrderService->InventoryService",
                "OrderService->OrderRepository",
                "InventoryService->AuditComponent"), edges);
        assertTrue(analysis.dependencies().stream().allMatch(dependency ->
                !dependency.evidence().isEmpty()
                        && Set.of("CONSTRUCTOR_INJECTION", "FIELD_INJECTION")
                                .contains(dependency.evidence().getFirst().type())));
        assertTrue(analysis.dependencies().stream().allMatch(dependency ->
                dependency.targetModulePomFileId().orElseThrow().equals("pom.xml")));
    }

    @Test
    void doesNotBindAnInjectedInterfaceToOneOfMultipleImplementations() {
        Path fixture = FixturePaths.project("spring-injection-project");
        Pipeline pipeline = analyze(fixture, temporaryDirectory.resolve("interface-cache"));

        SpringComponentDependencyAnalysis analysis = new SpringComponentDependencyAnalyzer().analyze(
                pipeline.components(), pipeline.injections());

        assertTrue(analysis.dependencies().stream().noneMatch(dependency ->
                dependency.targetQualifiedName().filter(name ->
                        name.endsWith("FastPort") || name.endsWith("SafePort")).isPresent()));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_COMPONENT_DEPENDENCY_UNCONFIRMED")));
    }

    private static String simple(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    private static Pipeline analyze(Path root, Path cache) {
        var maven = new MavenModuleAnalyzer().analyze(
                root, new MavenModelOptions(cache, List.of(), 1_048_576));
        var sourceRoots = new JavaSourceRootDiscoverer().discover(root, maven);
        var files = new JavaFileDiscoverer().discover(root, sourceRoots);
        JavaParseAnalysis parsed = new JavaSourceParser().parse(root, files);
        JavaParseAnalysis facts = new JavaTypeReferenceResolver().resolve(
                parsed, maven, JavaDeclarationIndex.build(parsed));
        SpringComponentAnalysis components = new SpringComponentAnalyzer().analyze(facts);
        SpringConfigurationAnalysis configurations = new SpringConfigurationAnalyzer().analyze(facts);
        SpringInjectionAnalysis injections = new SpringInjectionAnalyzer().analyze(
                facts, components, configurations);
        return new Pipeline(components, injections);
    }

    private record Pipeline(
            SpringComponentAnalysis components, SpringInjectionAnalysis injections) {
    }
}
