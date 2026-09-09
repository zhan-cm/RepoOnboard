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
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        List<SpringComponentDependencyFact> confirmed = analysis.dependencies().stream()
                .filter(dependency -> dependency.status() == SpringDependencyStatus.CONFIRMED)
                .toList();
        assertEquals(5, confirmed.size());
        assertTrue(confirmed.stream()
                .allMatch(dependency -> dependency.kind() == SpringDependencyKind.COMPONENT_INJECTION));
        Set<String> edges = confirmed.stream()
                .map(dependency -> simple(dependency.sourceQualifiedName()) + "->"
                        + simple(dependency.targetQualifiedName().orElseThrow()))
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "OrderController->OrderService",
                "OrderService->InventoryService",
                "OrderService->OrderRepository",
                "InventoryService->AuditComponent",
                "AuditComponent->OrderService"), edges);
        assertTrue(confirmed.stream().allMatch(dependency ->
                !dependency.evidence().isEmpty()
                        && Set.of("CONSTRUCTOR_INJECTION", "FIELD_INJECTION")
                                .contains(dependency.evidence().getFirst().type())));
        assertTrue(confirmed.stream().allMatch(dependency ->
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
        SpringComponentDependencyFact unresolvedInterface = analysis.dependencies().stream()
                .filter(dependency -> dependency.sourceQualifiedName().endsWith("CheckoutService"))
                .findFirst()
                .orElseThrow();
        assertEquals(SpringDependencyStatus.UNRESOLVED_TARGET, unresolvedInterface.status());
        assertEquals("fixture.inject.Port",
                unresolvedInterface.resolvedTypeQualifiedName().orElseThrow());
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_DEPENDENCY_TARGET_UNRESOLVED")));
    }

    @Test
    void consolidatesEvidenceRetainsUnresolvedTargetsAndSupportsCycles() {
        Path fixture = FixturePaths.project("spring-dependency-project");
        Pipeline pipeline = analyze(fixture, temporaryDirectory.resolve("consolidation-cache"));

        SpringComponentDependencyAnalysis analysis = new SpringComponentDependencyAnalyzer().analyze(
                pipeline.components(), pipeline.injections());

        SpringComponentDependencyFact duplicate = analysis.dependencies().stream()
                .filter(dependency -> dependency.sourceQualifiedName().endsWith("OrderController"))
                .filter(dependency -> dependency.targetQualifiedName().filter(
                        name -> name.endsWith("OrderService")).isPresent())
                .findFirst()
                .orElseThrow();
        assertEquals(2, duplicate.evidence().size());
        assertEquals(Set.of("CONSTRUCTOR_INJECTION", "FIELD_INJECTION"),
                duplicate.evidence().stream().map(evidence -> evidence.type())
                        .collect(Collectors.toSet()));

        SpringComponentDependencyFact unresolved = analysis.dependencies().stream()
                .filter(dependency -> dependency.sourceQualifiedName().endsWith("MissingClient"))
                .findFirst()
                .orElseThrow();
        assertEquals(SpringDependencyStatus.UNRESOLVED_TARGET, unresolved.status());
        assertEquals("ExternalGateway", unresolved.declaredTargetType());
        assertTrue(unresolved.targetQualifiedName().isEmpty());
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_DEPENDENCY_TARGET_UNRESOLVED")));

        Map<SpringComponentRef, List<SpringComponentRef>> adjacency = analysis.confirmedAdjacency();
        SpringComponentRef orderService = adjacency.keySet().stream()
                .filter(component -> component.qualifiedName().endsWith("OrderService"))
                .findFirst()
                .orElseThrow();
        Set<SpringComponentRef> visited = reachable(adjacency, orderService);
        assertTrue(visited.stream().anyMatch(component ->
                component.qualifiedName().endsWith("InventoryService")));
        assertTrue(visited.stream().anyMatch(component ->
                component.qualifiedName().endsWith("AuditComponent")));
        assertTrue(visited.stream().anyMatch(component ->
                component.qualifiedName().endsWith("OrderRepository")));
        assertEquals(4, visited.size());
    }

    @Test
    void retainsDuplicateQualifiedComponentTargetsAsAmbiguous() {
        SourceLocation ownerLocation = SourceLocation.file("owner/Owner.java");
        SourceLocation targetOneLocation = SourceLocation.file("one/Target.java");
        SourceLocation targetTwoLocation = SourceLocation.file("two/Target.java");
        Evidence ownerEvidence = new Evidence(
                "SPRING_COMPONENT_ANNOTATION", ownerLocation, List.of(), "test.owner");
        SpringComponentFact owner = new SpringComponentFact(
                "owner/pom.xml", "demo.Owner", Optional.of("owner"),
                SpringComponentKind.SERVICE, ownerLocation, List.of(ownerEvidence));
        SpringComponentFact targetOne = new SpringComponentFact(
                "one/pom.xml", "demo.Target", Optional.of("target"),
                SpringComponentKind.SERVICE, targetOneLocation,
                List.of(new Evidence("SPRING_COMPONENT_ANNOTATION", targetOneLocation,
                        List.of(), "test.target.one")));
        SpringComponentFact targetTwo = new SpringComponentFact(
                "two/pom.xml", "demo.Target", Optional.of("target"),
                SpringComponentKind.SERVICE, targetTwoLocation,
                List.of(new Evidence("SPRING_COMPONENT_ANNOTATION", targetTwoLocation,
                        List.of(), "test.target.two")));
        SpringInjectionCandidate injection = new SpringInjectionCandidate(
                "owner/pom.xml", "demo.Owner", SpringInjectionKind.CONSTRUCTOR,
                "target", "Target", SpringInjectionStatus.CONFIRMED,
                Optional.of("demo.Target"), ownerLocation,
                List.of(new Evidence("CONSTRUCTOR_INJECTION", ownerLocation,
                        List.of(), "test.injection")));

        SpringComponentDependencyAnalysis analysis = new SpringComponentDependencyAnalyzer().analyze(
                new SpringComponentAnalysis(List.of(owner, targetOne, targetTwo), List.of()),
                new SpringInjectionAnalysis(List.of(injection), List.of()));

        assertEquals(1, analysis.dependencies().size());
        assertEquals(SpringDependencyStatus.AMBIGUOUS_TARGET,
                analysis.dependencies().getFirst().status());
        assertTrue(analysis.dependencies().getFirst().targetQualifiedName().isEmpty());
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_DEPENDENCY_TARGET_AMBIGUOUS")));
    }

    private static Set<SpringComponentRef> reachable(
            Map<SpringComponentRef, List<SpringComponentRef>> adjacency,
            SpringComponentRef start) {
        Set<SpringComponentRef> visited = new HashSet<>();
        ArrayDeque<SpringComponentRef> pending = new ArrayDeque<>();
        pending.add(start);
        while (!pending.isEmpty()) {
            SpringComponentRef current = pending.removeFirst();
            if (visited.add(current)) {
                pending.addAll(adjacency.getOrDefault(current, List.of()));
            }
        }
        return visited;
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
