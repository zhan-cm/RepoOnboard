package io.github.zhancm.repoonboard.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analysis.ProjectAnalysisInput;
import io.github.zhancm.repoonboard.analysis.ProjectModelAssembler;
import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceStatus;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalysis;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringInjectionAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringMvcMappingAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiModuleRegressionFixtureTest {

    private static final String ROOT_MODULE_ID = "module:pom.xml";
    private static final String API_MODULE_ID = "module:api%2Fpom.xml";
    private static final String LIBRARY_MODULE_ID = "module:library%2Fpom.xml";

    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsInheritanceAggregationOwnershipVisibilityAndEdgesDistinct() throws IOException {
        Path fixture = FixturePaths.project("multi-module-project");
        Set<String> filesBefore = relativeFiles(fixture);
        Path emptyLocalRepository = temporaryDirectory.resolve("empty-success-repository");

        Pipeline first = analyze(fixture, emptyLocalRepository);
        Pipeline repeated = analyze(fixture, emptyLocalRepository);

        assertEquals(first, repeated);
        assertEquals(AnalysisStatus.SUCCESS, first.report().status());
        assertTrue(first.report().diagnostics().isEmpty());

        MavenModule root = first.maven().root().orElseThrow();
        assertEquals("pom.xml", root.pomFileId());
        assertEquals(Optional.empty(), root.parentCoordinates());
        assertEquals(List.of("api/pom.xml", "library/pom.xml"), root.children().stream()
                .map(MavenModule::pomFileId)
                .toList());
        MavenModule api = root.children().getFirst();
        MavenModule library = root.children().get(1);
        assertEquals(Optional.of("example:workspace:1"), api.parentCoordinates());
        assertEquals(Optional.empty(), library.parentCoordinates());
        assertEquals("pom.xml", api.metadata().groupId().origin().sourceFileId());
        assertEquals("library/pom.xml", library.metadata().groupId().origin().sourceFileId());
        assertEquals("api/code/java", api.sourceDirectory().resolvedValue().orElseThrow());
        assertEquals("library/src/main/java",
                library.sourceDirectory().resolvedValue().orElseThrow());

        var unitsByPath = first.java().compilationUnits().stream().collect(Collectors.toMap(
                unit -> unit.sourceFile().relativePath(), Function.identity()));
        assertEquals(Set.of(
                        "api/code/java/app/ApiType.java",
                        "library/src/main/java/shared/LibraryType.java"),
                unitsByPath.keySet());
        var apiUnit = unitsByPath.get("api/code/java/app/ApiType.java");
        assertEquals("api/pom.xml", apiUnit.sourceFile().modulePomFileId());
        assertEquals("api/code/java", apiUnit.sourceFile().sourceRootRelativePath());
        assertEquals("library/pom.xml", unitsByPath
                .get("library/src/main/java/shared/LibraryType.java")
                .sourceFile().modulePomFileId());
        List<JavaTypeReferenceFact> libraryReferences = apiUnit.typeReferences().stream()
                .filter(reference -> reference.name().equals("LibraryType"))
                .toList();
        assertEquals(2, libraryReferences.size());
        assertTrue(libraryReferences.stream().allMatch(reference ->
                reference.status() == JavaTypeReferenceStatus.RESOLVED
                        && reference.resolvedQualifiedName().equals(Optional.of("shared.LibraryType"))));

        Map<String, io.github.zhancm.repoonboard.core.model.Module> modulesByPom =
                first.report().modules().stream().collect(Collectors.toMap(
                        io.github.zhancm.repoonboard.core.model.Module::pomFileId,
                        Function.identity()));
        assertEquals(ROOT_MODULE_ID, modulesByPom.get("pom.xml").id());
        assertEquals(API_MODULE_ID, modulesByPom.get("api/pom.xml").id());
        assertEquals(LIBRARY_MODULE_ID, modulesByPom.get("library/pom.xml").id());
        assertEquals(Optional.empty(), modulesByPom.get("pom.xml").aggregationParentModuleId());
        assertEquals(Optional.of(ROOT_MODULE_ID),
                modulesByPom.get("api/pom.xml").aggregationParentModuleId());
        assertEquals(Optional.of(ROOT_MODULE_ID),
                modulesByPom.get("library/pom.xml").aggregationParentModuleId());
        assertEquals(List.of("api/code/java"), modulesByPom.get("api/pom.xml").sourceRoots());
        assertEquals(List.of("library/src/main/java"),
                modulesByPom.get("library/pom.xml").sourceRoots());
        assertTrue(modulesByPom.get("api/pom.xml").evidence().stream().anyMatch(evidence ->
                evidence.type().equals("MAVEN_MODULE_AGGREGATION")
                        && evidence.location().sourceFileId().equals("pom.xml")));

        Map<String, io.github.zhancm.repoonboard.core.model.SourceFile> sourcesByPath =
                first.report().sourceFiles().stream().collect(Collectors.toMap(
                        io.github.zhancm.repoonboard.core.model.SourceFile::path,
                        Function.identity()));
        assertEquals("source:api%2Fcode%2Fjava%2Fapp%2FApiType.java",
                sourcesByPath.get("api/code/java/app/ApiType.java").id());
        assertEquals(API_MODULE_ID,
                sourcesByPath.get("api/code/java/app/ApiType.java").moduleId());
        assertEquals("source:library%2Fsrc%2Fmain%2Fjava%2Fshared%2FLibraryType.java",
                sourcesByPath.get("library/src/main/java/shared/LibraryType.java").id());
        assertEquals(LIBRARY_MODULE_ID,
                sourcesByPath.get("library/src/main/java/shared/LibraryType.java").moduleId());

        var internalEdge = first.report().dependencies().stream()
                .filter(dependency -> dependency.kind() == DependencyKind.MAVEN_DECLARATION)
                .filter(dependency -> dependency.sourceId().equals(API_MODULE_ID))
                .findFirst()
                .orElseThrow();
        assertEquals("dependency:272eed40b6af77ea080879b8", internalEdge.id());
        assertEquals(Optional.of(LIBRARY_MODULE_ID), internalEdge.targetId());
        assertEquals("independent:library:2", internalEdge.declaredTarget());
        assertEquals(ResolutionStatus.CONFIRMED, internalEdge.status());
        assertEquals("api/pom.xml", internalEdge.location().sourceFileId());
        assertEquals(18, internalEdge.location().startLine().orElseThrow());
        assertEquals(List.of("MAVEN_DEPENDENCY"),
                internalEdge.evidence().stream().map(Evidence::type).toList());
        assertEquals(List.of("maven.dependency.declaration"),
                internalEdge.evidence().stream().map(Evidence::ruleId).toList());

        assertEquals(filesBefore, relativeFiles(fixture));
        assertFalse(Files.exists(fixture.resolve("target")));
        assertFalse(Files.exists(emptyLocalRepository));
    }

    @Test
    void retainsAggregatedChildrenWhenExternalParentAndBomAreUnavailable() throws IOException {
        Path fixture = FixturePaths.project("multi-module-partial-project");
        Set<String> filesBefore = relativeFiles(fixture);
        Path emptyLocalRepository = temporaryDirectory.resolve("empty-partial-repository");

        Pipeline result = analyze(fixture, emptyLocalRepository);

        assertEquals(AnalysisStatus.PARTIAL, result.maven().status());
        assertEquals(AnalysisStatus.PARTIAL, result.report().status());
        MavenModule root = result.maven().root().orElseThrow();
        assertEquals(List.of("missing-parent/pom.xml", "missing-bom/pom.xml"),
                root.children().stream().map(MavenModule::pomFileId).toList());
        MavenModule missingParent = root.children().getFirst();
        MavenModule missingBom = root.children().get(1);
        assertEquals(Optional.of("missing.external:base:1"), missingParent.parentCoordinates());
        assertEquals("missing-parent",
                missingParent.metadata().artifactId().rawValue().orElseThrow());
        assertEquals("fixture", missingBom.metadata().groupId().rawValue().orElseThrow());
        assertEquals("missing-bom", missingBom.metadata().artifactId().rawValue().orElseThrow());

        Map<String, io.github.zhancm.repoonboard.core.model.Module> modulesByPom =
                result.report().modules().stream().collect(Collectors.toMap(
                        io.github.zhancm.repoonboard.core.model.Module::pomFileId,
                        Function.identity()));
        assertEquals(Set.of("pom.xml", "missing-parent/pom.xml", "missing-bom/pom.xml"),
                modulesByPom.keySet());
        assertEquals(Optional.of(ROOT_MODULE_ID),
                modulesByPom.get("missing-parent/pom.xml").aggregationParentModuleId());
        assertEquals(Optional.of(ROOT_MODULE_ID),
                modulesByPom.get("missing-bom/pom.xml").aggregationParentModuleId());
        assertTrue(hasLocatedModelProblem(result.report(), "missing-parent/pom.xml"));
        assertTrue(hasLocatedModelProblem(result.report(), "missing-bom/pom.xml"));

        assertEquals(filesBefore, relativeFiles(fixture));
        assertFalse(Files.exists(fixture.resolve("target")));
        assertFalse(Files.exists(emptyLocalRepository));
    }

    private static boolean hasLocatedModelProblem(AnalysisReport report, String fileId) {
        return report.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("MAVEN_MODEL_PROBLEM")
                        && diagnostic.fileId().equals(Optional.of(fileId))
                        && diagnostic.location().map(location ->
                                location.sourceFileId().equals(fileId)
                                        && location.startLine().isPresent()).orElse(false));
    }

    private static Pipeline analyze(Path fixture, Path localRepository) {
        MavenModuleAnalysis maven = new MavenModuleAnalyzer().analyze(
                fixture, new MavenModelOptions(localRepository, List.of(), 1_048_576));
        var roots = new JavaSourceRootDiscoverer().discover(fixture, maven);
        var files = new JavaFileDiscoverer().discover(fixture, roots);
        JavaParseAnalysis java = new JavaTypeReferenceResolver().resolve(
                new JavaSourceParser().parse(fixture, files), maven);
        var components = new SpringComponentAnalyzer().analyze(java);
        var configuration = new SpringConfigurationAnalyzer().analyze(java);
        var injection = new SpringInjectionAnalyzer().analyze(java, components, configuration);
        var mappings = new SpringMvcMappingAnalyzer().analyze(java, components);
        var endpoints = new SpringEndpointAnalyzer().analyze(mappings);
        var dependencies = new SpringComponentDependencyAnalyzer().analyze(components, injection);
        AnalysisReport report = new ProjectModelAssembler().assemble(new ProjectAnalysisInput(
                maven, java, components, configuration, endpoints, dependencies));
        return new Pipeline(maven, java, report);
    }

    private static Set<String> relativeFiles(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private record Pipeline(
            MavenModuleAnalysis maven,
            JavaParseAnalysis java,
            AnalysisReport report) {
    }
}
