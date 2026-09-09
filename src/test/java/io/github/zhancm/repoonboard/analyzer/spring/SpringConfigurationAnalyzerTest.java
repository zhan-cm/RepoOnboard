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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringConfigurationAnalyzerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void detectsConfigurationAndApplicationEntryPointWithEvidence() {
        Path fixture = FixturePaths.project("spring-analysis-project");

        SpringConfigurationAnalysis analysis = new SpringConfigurationAnalyzer().analyze(
                parse(fixture, temporaryDirectory.resolve("cache")));

        assertEquals(AnalysisStatus.SUCCESS, analysis.status());
        assertEquals(
                List.of("fixture.Application", "fixture.config.AppConfiguration"),
                analysis.configurations().stream()
                        .map(SpringConfigurationFact::qualifiedName)
                        .toList());
        assertEquals(1, analysis.entryPoints().size());
        SpringEntryPointFact entryPoint = analysis.entryPoints().getFirst();
        assertEquals("fixture.Application", entryPoint.qualifiedName());
        assertEquals(SpringEntryPointKind.APPLICATION, entryPoint.kind());
        assertEquals("pom.xml", entryPoint.modulePomFileId());
        assertEquals("src/main/java/fixture/Application.java", entryPoint.location().sourceFileId());
        assertEquals("SPRING_BOOT_APPLICATION_ANNOTATION", entryPoint.evidence().getFirst().type());
        assertEquals(5, entryPoint.evidence().getFirst().location().startLine().orElseThrow());
        assertFalse(analysis.configurations().stream().anyMatch(configuration ->
                configuration.qualifiedName().equals("fixture.fake.FakeApplication")));
    }

    @Test
    void ambiguousWildcardConfigurationAnnotationIsNotGuessed() throws IOException {
        Files.writeString(temporaryDirectory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId>
                <artifactId>ambiguous-config</artifactId><version>1</version></project>
                """);
        Path source = temporaryDirectory.resolve("src/main/java/demo/AmbiguousConfiguration.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package demo;
                import org.springframework.context.annotation.*;
                import example.other.*;
                @Configuration class AmbiguousConfiguration {}
                """);

        SpringConfigurationAnalysis analysis = new SpringConfigurationAnalyzer().analyze(
                parse(temporaryDirectory, temporaryDirectory.resolve("cache")));

        assertEquals(AnalysisStatus.PARTIAL, analysis.status());
        assertTrue(analysis.configurations().isEmpty());
        assertTrue(analysis.entryPoints().isEmpty());
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_CONFIGURATION_ANNOTATION_AMBIGUOUS")));
    }

    @Test
    void detectsProjectLocalComposedConfiguration() throws IOException {
        Files.writeString(temporaryDirectory.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId>
                <artifactId>composed-config</artifactId><version>1</version></project>
                """);
        Path annotation = temporaryDirectory.resolve("src/main/java/demo/ModuleConfiguration.java");
        Files.createDirectories(annotation.getParent());
        Files.writeString(annotation, """
                package demo;
                import org.springframework.context.annotation.Configuration;
                @Configuration public @interface ModuleConfiguration {}
                """);
        Files.writeString(annotation.getParent().resolve("FeatureConfiguration.java"), """
                package demo;
                @ModuleConfiguration public class FeatureConfiguration {}
                """);

        SpringConfigurationAnalysis analysis = new SpringConfigurationAnalyzer().analyze(
                parse(temporaryDirectory, temporaryDirectory.resolve("cache")));

        assertEquals(AnalysisStatus.SUCCESS, analysis.status());
        assertEquals(1, analysis.configurations().size());
        assertEquals("demo.FeatureConfiguration",
                analysis.configurations().getFirst().qualifiedName());
        assertEquals("SPRING_COMPOSED_CONFIGURATION_ANNOTATION",
                analysis.configurations().getFirst().evidence().getFirst().type());
        assertTrue(analysis.entryPoints().isEmpty());
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
}
