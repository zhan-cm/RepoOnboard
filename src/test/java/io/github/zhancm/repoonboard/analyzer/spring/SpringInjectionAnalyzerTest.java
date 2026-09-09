package io.github.zhancm.repoonboard.analyzer.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analyzer.java.JavaDeclarationIndex;
import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringInjectionAnalyzerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void extractsConfirmedConstructorAndFieldInjectionWithoutChoosingImplementations() {
        SpringInjectionAnalysis analysis = analyzeFixture();

        SpringInjectionCandidate checkoutPort = candidate(
                analysis, "fixture.inject.CheckoutService", "port");
        assertEquals(SpringInjectionKind.CONSTRUCTOR, checkoutPort.kind());
        assertEquals(SpringInjectionStatus.CONFIRMED, checkoutPort.status());
        assertEquals("fixture.inject.Port", checkoutPort.targetQualifiedName().orElseThrow());
        assertFalse(checkoutPort.targetQualifiedName().orElseThrow().contains("FastPort"));
        assertFalse(checkoutPort.targetQualifiedName().orElseThrow().contains("SafePort"));
        assertEquals("CONSTRUCTOR_INJECTION", checkoutPort.evidence().getFirst().type());

        SpringInjectionCandidate controller = candidate(
                analysis, "fixture.inject.OrderController", "service");
        assertEquals(SpringInjectionKind.CONSTRUCTOR, controller.kind());
        assertEquals(SpringInjectionStatus.CONFIRMED, controller.status());
        assertEquals("fixture.inject.CheckoutService", controller.targetQualifiedName().orElseThrow());
        assertEquals("spring.injection.autowired_constructor", controller.evidence().getFirst().ruleId());

        SpringInjectionCandidate field = candidate(analysis, "fixture.inject.FieldClient", "service");
        assertEquals(SpringInjectionKind.FIELD, field.kind());
        assertEquals(SpringInjectionStatus.CONFIRMED, field.status());
        assertEquals("fixture.inject.CheckoutService", field.targetQualifiedName().orElseThrow());
        assertEquals("FIELD_INJECTION", field.evidence().getFirst().type());
        assertFalse(analysis.candidates().stream().anyMatch(candidate ->
                candidate.ownerQualifiedName().equals("fixture.inject.FakeFieldClient")));
    }

    @Test
    void retainsAmbiguousConstructorsAndReportsUnsupportedInjectionPatterns() {
        SpringInjectionAnalysis analysis = analyzeFixture();

        assertEquals(AnalysisStatus.PARTIAL, analysis.status());
        List<SpringInjectionCandidate> ambiguous = analysis.candidates().stream()
                .filter(candidate -> candidate.ownerQualifiedName().equals("fixture.inject.AmbiguousClient"))
                .toList();
        assertEquals(2, ambiguous.size());
        assertTrue(ambiguous.stream().allMatch(candidate ->
                candidate.status() == SpringInjectionStatus.AMBIGUOUS_CONSTRUCTOR));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_CONSTRUCTOR_INJECTION_AMBIGUOUS")));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_METHOD_INJECTION_UNSUPPORTED")));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_LOMBOK_CONSTRUCTOR_UNSUPPORTED")));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_STATIC_FIELD_INJECTION_IGNORED")));
        assertFalse(analysis.candidates().stream().anyMatch(candidate ->
                candidate.ownerQualifiedName().equals("fixture.inject.StaticClient")));
    }

    private SpringInjectionAnalysis analyzeFixture() {
        Path fixture = FixturePaths.project("spring-injection-project");
        var maven = new MavenModuleAnalyzer().analyze(
                fixture,
                new MavenModelOptions(temporaryDirectory.resolve("cache"), List.of(), 1_048_576));
        var roots = new JavaSourceRootDiscoverer().discover(fixture, maven);
        var files = new JavaFileDiscoverer().discover(fixture, roots);
        JavaParseAnalysis parsed = new JavaSourceParser().parse(fixture, files);
        JavaParseAnalysis javaFacts = new JavaTypeReferenceResolver().resolve(
                parsed, maven, JavaDeclarationIndex.build(parsed));
        SpringComponentAnalysis components = new SpringComponentAnalyzer().analyze(javaFacts);
        SpringConfigurationAnalysis configurations = new SpringConfigurationAnalyzer().analyze(javaFacts);
        return new SpringInjectionAnalyzer().analyze(javaFacts, components, configurations);
    }

    private static SpringInjectionCandidate candidate(
            SpringInjectionAnalysis analysis, String owner, String point) {
        return analysis.candidates().stream()
                .filter(candidate -> candidate.ownerQualifiedName().equals(owner)
                        && candidate.injectionPointName().equals(point))
                .findFirst()
                .orElseThrow();
    }
}
