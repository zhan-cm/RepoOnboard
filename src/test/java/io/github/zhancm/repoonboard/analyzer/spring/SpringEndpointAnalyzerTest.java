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
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringEndpointAnalyzerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void combinesPathsMethodsConditionsAndEvidence() {
        Path fixture = FixturePaths.project("spring-api-project");
        JavaParseAnalysis javaFacts = parse(fixture, temporaryDirectory.resolve("cache"));
        SpringComponentAnalysis components = new SpringComponentAnalyzer().analyze(javaFacts);
        SpringMappingAnalysis mappings = new SpringMvcMappingAnalyzer().analyze(javaFacts, components);

        SpringEndpointAnalysis analysis = new SpringEndpointAnalyzer().analyze(mappings);

        assertEquals(AnalysisStatus.SUCCESS, analysis.status());
        assertEquals(17, analysis.endpoints().size());
        Set<String> listPaths = endpoints(analysis, "list").stream()
                .map(endpoint -> endpoint.path().orElseThrow())
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "/api/users", "/api/members", "/internal/users", "/internal/members"), listPaths);
        assertTrue(endpoints(analysis, "list").stream()
                .allMatch(endpoint -> endpoint.httpMethod() == SpringHttpMethod.GET));

        SpringEndpointFact list = endpoints(analysis, "list").getFirst();
        assertEquals(List.of("X-Tenant"), list.conditions().headers());
        assertEquals(List.of("active=true"), list.conditions().params());
        assertEquals(2, list.evidence().size());
        assertEquals("src/main/java/fixture/api/UserController.java", list.location().sourceFileId());
        assertEquals(14, list.location().startLine().orElseThrow());

        Set<SpringHttpMethod> searchMethods = endpoints(analysis, "search").stream()
                .map(SpringEndpointFact::httpMethod)
                .collect(Collectors.toSet());
        assertEquals(Set.of(SpringHttpMethod.GET, SpringHttpMethod.POST), searchMethods);
        assertTrue(endpoints(analysis, "anyMethod").stream()
                .allMatch(endpoint -> endpoint.httpMethod() == SpringHttpMethod.ANY));

        List<SpringEndpointFact> unresolved = endpoints(analysis, "unresolvedPath");
        assertEquals(1, unresolved.size());
        assertTrue(unresolved.getFirst().unresolvedPath());
        assertTrue(unresolved.getFirst().path().isEmpty());

        SpringEndpointFact active = endpoints(analysis, "filteredActive").stream()
                .filter(endpoint -> endpoint.path().orElseThrow().equals("/api/filtered"))
                .findFirst()
                .orElseThrow();
        SpringEndpointFact inactive = endpoints(analysis, "filteredInactive").stream()
                .filter(endpoint -> endpoint.path().orElseThrow().equals("/api/filtered"))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("state=active"), active.conditions().params());
        assertEquals(List.of("state=inactive"), inactive.conditions().params());
        assertFalse(active.conditions().equals(inactive.conditions()));
    }

    private static List<SpringEndpointFact> endpoints(
            SpringEndpointAnalysis analysis, String handler) {
        return analysis.endpoints().stream()
                .filter(endpoint -> endpoint.handlerMethod().equals(handler))
                .toList();
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
