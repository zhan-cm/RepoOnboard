package io.github.zhancm.repoonboard.analyzer.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaDeclarationIndex;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringComponentAnalyzerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void classifiesStandardComponentsNamesAndEvidenceFromFixture() {
        Path fixture = FixturePaths.project("spring-analysis-project");

        SpringComponentAnalysis analysis = new SpringComponentAnalyzer().analyze(
                parse(fixture, temporaryDirectory.resolve("cache")));

        assertEquals(AnalysisStatus.SUCCESS, analysis.status());
        assertEquals(6, analysis.components().size());
        Map<String, SpringComponentFact> components = analysis.components().stream()
                .collect(Collectors.toMap(SpringComponentFact::qualifiedName, Function.identity()));
        assertComponent(components, "fixture.web.PageController", SpringComponentKind.CONTROLLER,
                "pageController");
        assertComponent(components, "fixture.web.UserController", SpringComponentKind.REST_CONTROLLER,
                "users");
        assertComponent(components, "fixture.service.UserService", SpringComponentKind.SERVICE,
                "userService");
        assertComponent(components, "fixture.repository.UserRepository", SpringComponentKind.REPOSITORY,
                "userRepository");
        assertComponent(components, "fixture.component.ClockComponent", SpringComponentKind.COMPONENT,
                "clock");
        assertComponent(components, "fixture.component.URLClient", SpringComponentKind.COMPONENT,
                "URLClient");
        assertFalse(components.containsKey("fixture.fake.FakeService"));

        SpringComponentFact controller = components.get("fixture.web.UserController");
        assertEquals("pom.xml", controller.modulePomFileId());
        assertEquals("src/main/java/fixture/web/UserController.java",
                controller.location().sourceFileId());
        assertTrue(controller.location().startLine().isPresent());
        assertEquals("SPRING_COMPONENT_ANNOTATION", controller.evidence().getFirst().type());
        assertEquals(8, controller.evidence().getFirst().location().startLine().orElseThrow());
    }

    @Test
    void ambiguousAndConflictingAnnotationsAreNotGuessed() throws IOException {
        Files.writeString(temporaryDirectory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId>
                <artifactId>ambiguous</artifactId><version>1</version></project>
                """);
        write("src/main/java/demo/Ambiguous.java", """
                package demo;
                import org.springframework.stereotype.*;
                import example.other.*;
                @Service class Ambiguous {}
                """);
        write("src/main/java/demo/Conflicting.java", """
                package demo;
                import org.springframework.stereotype.Controller;
                import org.springframework.stereotype.Service;
                @Controller @Service class Conflicting {}
                """);
        write("src/main/java/demo/Named.java", """
                package demo;
                @org.springframework.stereotype.Component(NAME) class Named {}
                """);

        SpringComponentAnalysis analysis = new SpringComponentAnalyzer().analyze(
                parse(temporaryDirectory, temporaryDirectory.resolve("cache")));

        assertEquals(AnalysisStatus.PARTIAL, analysis.status());
        assertEquals(1, analysis.components().size());
        assertEquals("demo.Named", analysis.components().getFirst().qualifiedName());
        assertTrue(analysis.components().getFirst().name().isEmpty());
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_COMPONENT_ANNOTATION_AMBIGUOUS")));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_COMPONENT_KIND_AMBIGUOUS")));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_COMPONENT_NAME_UNRESOLVED")));
    }

    @Test
    void resolvesProjectLocalCompositionAndRejectsConflictingRoles() throws IOException {
        Files.writeString(temporaryDirectory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId>
                <artifactId>composed</artifactId><version>1</version></project>
                """);
        write("src/main/java/demo/ApplicationService.java", """
                package demo;
                import org.springframework.stereotype.Service;
                @Service public @interface ApplicationService {}
                """);
        write("src/main/java/demo/ConflictingRole.java", """
                package demo;
                import org.springframework.stereotype.Repository;
                import org.springframework.stereotype.Service;
                @Repository @Service public @interface ConflictingRole {}
                """);
        write("src/main/java/demo/CheckoutService.java", """
                package demo;
                @ApplicationService public class CheckoutService {}
                """);
        write("src/main/java/demo/ConflictingService.java", """
                package demo;
                @ConflictingRole public class ConflictingService {}
                """);

        SpringComponentAnalysis analysis = new SpringComponentAnalyzer().analyze(
                parse(temporaryDirectory, temporaryDirectory.resolve("cache")));

        assertEquals(AnalysisStatus.PARTIAL, analysis.status());
        assertEquals(1, analysis.components().size());
        SpringComponentFact component = analysis.components().getFirst();
        assertEquals("demo.CheckoutService", component.qualifiedName());
        assertEquals(SpringComponentKind.SERVICE, component.kind());
        assertEquals("checkoutService", component.name().orElseThrow());
        assertEquals("SPRING_COMPOSED_COMPONENT_ANNOTATION", component.evidence().getFirst().type());
        assertEquals(2, component.evidence().getFirst().relatedLocations().size());
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_COMPONENT_KIND_AMBIGUOUS")));
    }

    private static void assertComponent(
            Map<String, SpringComponentFact> components,
            String qualifiedName,
            SpringComponentKind kind,
            String name) {
        SpringComponentFact component = components.get(qualifiedName);
        assertEquals(kind, component.kind());
        assertEquals(name, component.name().orElseThrow());
    }

    private static JavaParseAnalysis parse(Path root, Path cache) {
        var maven = new MavenModuleAnalyzer().analyze(
                root, new MavenModelOptions(cache, List.of(), 1_048_576));
        var sourceRoots = new JavaSourceRootDiscoverer().discover(root, maven);
        var files = new JavaFileDiscoverer().discover(root, sourceRoots);
        JavaParseAnalysis parsed = new JavaSourceParser().parse(root, files);
        return new JavaTypeReferenceResolver().resolve(
                parsed, maven, JavaDeclarationIndex.build(parsed));
    }

    private void write(String relativePath, String contents) throws IOException {
        Path file = temporaryDirectory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
    }
}
