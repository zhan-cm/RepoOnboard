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

class SpringMvcMappingAnalyzerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void extractsConfirmedTypeAndMethodMappingsWithConditions() {
        Path fixture = FixturePaths.project("spring-api-project");
        JavaParseAnalysis javaFacts = parse(fixture, temporaryDirectory.resolve("cache"));
        SpringComponentAnalysis components = new SpringComponentAnalyzer().analyze(javaFacts);

        SpringMappingAnalysis analysis = new SpringMvcMappingAnalyzer().analyze(javaFacts, components);

        assertEquals(AnalysisStatus.PARTIAL, analysis.status());
        assertEquals(8, analysis.mappings().size());
        SpringMappingFact typeMapping = analysis.mappings().stream()
                .filter(mapping -> mapping.level() == SpringMappingLevel.TYPE)
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("/api", "/internal"), typeMapping.paths());
        assertEquals(List.of(SpringHttpMethod.ANY), typeMapping.httpMethods());
        assertEquals(List.of("X-Tenant"), typeMapping.conditions().headers());
        assertEquals("SPRING_MVC_TYPE_MAPPING", typeMapping.evidence().getFirst().type());

        SpringMappingFact list = mapping(analysis, "list");
        assertEquals(List.of("/users", "/members"), list.paths());
        assertEquals(List.of(SpringHttpMethod.GET), list.httpMethods());
        assertEquals(List.of("active=true"), list.conditions().params());
        assertEquals(List.of("application/json"), list.conditions().produces());
        assertFalse(list.unresolvedPath());

        SpringMappingFact search = mapping(analysis, "search");
        assertEquals(List.of(SpringHttpMethod.GET, SpringHttpMethod.POST), search.httpMethods());
        assertEquals(List.of("application/json"), search.conditions().consumes());
        assertFalse(search.unresolvedMethod());

        assertEquals(List.of(SpringHttpMethod.ANY), mapping(analysis, "anyMethod").httpMethods());

        assertTrue(mapping(analysis, "unresolvedPath").unresolvedPath());
        assertTrue(mapping(analysis, "unresolvedCondition").conditions().unresolved());
        assertTrue(analysis.mappings().stream().noneMatch(mapping ->
                mapping.controllerQualifiedName().equals("fixture.api.FakeController")));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_MVC_PATH_UNRESOLVED")));
        assertTrue(analysis.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("SPRING_MVC_CONDITION_UNRESOLVED")));
    }

    private static SpringMappingFact mapping(SpringMappingAnalysis analysis, String handler) {
        return analysis.mappings().stream()
                .filter(mapping -> mapping.handlerMethod().filter(handler::equals).isPresent())
                .findFirst()
                .orElseThrow();
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
