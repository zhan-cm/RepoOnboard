package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.*;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenDependencyExtractorTest {
    @TempDir Path root;

    @Test
    void extractsCoordinatesPropertiesAndDefaultScope() throws Exception {
        write("pom.xml", pom("""
                <properties><library.version>2.1</library.version></properties>
                <dependencies><dependency><groupId>example</groupId><artifactId>library</artifactId>
                <version>${library.version}</version></dependency>
                <dependency><groupId>example</groupId><artifactId>tests</artifactId>
                <version>1</version><scope>test</scope></dependency></dependencies>
                """));
        var result = analyze();
        assertEquals(AnalysisStatus.SUCCESS, result.status());
        var deps = result.root().orElseThrow().dependencies();
        assertEquals(2, deps.size());
        assertEquals("example", deps.getFirst().groupId().resolvedValue().orElseThrow());
        assertEquals("library", deps.getFirst().artifactId().resolvedValue().orElseThrow());
        assertEquals("${library.version}", deps.getFirst().version().rawValue().orElseThrow());
        assertEquals("2.1", deps.getFirst().version().resolvedValue().orElseThrow());
        assertEquals("compile", deps.getFirst().scope().resolvedValue().orElseThrow());
        assertTrue(deps.getFirst().scope().rawValue().isEmpty());
        assertEquals("test", deps.get(1).scope().resolvedValue().orElseThrow());
        assertTrue(deps.getFirst().version().origin().startLine().isPresent());
    }

    @Test
    void localBomSuppliesVersionButUnusedManagedEntriesAreNotDependencies() throws Exception {
        write("cache/example/platform/1/platform-1.pom", """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>platform</artifactId><version>1</version><packaging>pom</packaging>
                <properties><managed.version>3</managed.version></properties>
                <dependencyManagement><dependencies><dependency><groupId>example</groupId>
                <artifactId>library</artifactId><version>${managed.version}</version><scope>runtime</scope>
                </dependency><dependency><groupId>example</groupId><artifactId>unused</artifactId>
                <version>1</version></dependency></dependencies></dependencyManagement></project>
                """);
        write("pom.xml", pom("""
                <dependencyManagement><dependencies><dependency><groupId>example</groupId>
                <artifactId>platform</artifactId><version>1</version><type>pom</type><scope>import</scope>
                </dependency></dependencies></dependencyManagement>
                <dependencies><dependency><groupId>example</groupId><artifactId>library</artifactId>
                </dependency></dependencies>
                """));
        var result = analyze();
        assertEquals(AnalysisStatus.SUCCESS, result.status(), result.diagnostics().toString());
        var deps = result.root().orElseThrow().dependencies();
        assertEquals(1, deps.size());
        assertEquals("3", deps.getFirst().version().resolvedValue().orElseThrow());
        assertEquals("${managed.version}", deps.getFirst().version().rawValue().orElseThrow());
        assertEquals("runtime", deps.getFirst().scope().resolvedValue().orElseThrow());
        assertEquals("local-repository/example/platform/1/platform-1.pom",
                deps.getFirst().version().origin().sourceFileId());
    }

    @Test
    void missingParentKeepsRawDependenciesAndUnresolvedVersions() throws Exception {
        write("pom.xml", """
                <project><modelVersion>4.0.0</modelVersion><parent><groupId>missing</groupId>
                <artifactId>parent</artifactId><version>1</version><relativePath/></parent><artifactId>app</artifactId>
                <dependencies><dependency><groupId>example</groupId><artifactId>library</artifactId>
                <version>${unknown}</version></dependency><dependency><groupId>example</groupId>
                <artifactId>unmanaged</artifactId></dependency></dependencies></project>
                """);
        var result = analyze();
        assertEquals(AnalysisStatus.PARTIAL, result.status());
        var deps = result.root().orElseThrow().dependencies();
        assertEquals(2, deps.size());
        assertEquals(MavenResolutionStatus.UNRESOLVED, deps.getFirst().version().resolutionStatus());
        assertEquals("${unknown}", deps.getFirst().version().rawValue().orElseThrow());
        assertEquals(MavenResolutionStatus.MISSING, deps.get(1).version().resolutionStatus());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code().equals("MAVEN_DEPENDENCY_FIELD_UNRESOLVED")));
    }

    @Test
    void inheritsParentDependenciesAndSelectsProfileDependencies() throws Exception {
        write("pom.xml", pom("""
                <packaging>pom</packaging><modules><module>child</module></modules>
                <dependencies><dependency><groupId>example</groupId><artifactId>inherited</artifactId>
                <version>1</version><scope>provided</scope></dependency></dependencies>
                """));
        write("child/pom.xml", """
                <project><modelVersion>4.0.0</modelVersion><parent><groupId>example</groupId>
                <artifactId>app</artifactId><version>1</version></parent><artifactId>child</artifactId>
                <profiles><profile><id>extra</id><dependencies><dependency><groupId>example</groupId>
                <artifactId>profile-lib</artifactId><version>2</version></dependency></dependencies></profile></profiles>
                </project>
                """);
        assertEquals(1, analyze().root().orElseThrow().children().getFirst().dependencies().size());
        var result = new MavenModuleAnalyzer().analyze(root,
                new MavenModelOptions(root.resolve("cache"), List.of("extra"), 1_048_576));
        assertEquals(AnalysisStatus.SUCCESS, result.status());
        var dependencies = result.root().orElseThrow().children().getFirst().dependencies();
        assertEquals(2, dependencies.size());
        var inherited = dependencies.stream().filter(d -> d.artifactId().resolvedValue().orElseThrow()
                .equals("inherited")).findFirst().orElseThrow();
        assertEquals("pom.xml", inherited.version().origin().sourceFileId());
        assertEquals("provided", inherited.scope().resolvedValue().orElseThrow());
    }

    private MavenModuleAnalysis analyze() {
        return new MavenModuleAnalyzer().analyze(root,
                new MavenModelOptions(root.resolve("cache"), List.of(), 1_048_576));
    }

    private String pom(String contents) {
        return "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>"
                + "<artifactId>app</artifactId><version>1</version>" + contents + "</project>";
    }

    private void write(String path, String contents) throws Exception {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
    }
}
