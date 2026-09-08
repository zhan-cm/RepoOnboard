package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.*;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenModuleAnalyzerTest {
    @TempDir Path root;

    @Test
    void fixedFixtureHasStableHierarchyAndSourceEvidence() {
        Path fixture = io.github.zhancm.repoonboard.testing.FixturePaths.project("multi-module-project");
        var options = new MavenModelOptions(root.resolve("cache"), List.of(), 1_048_576);
        var analyzer = new MavenModuleAnalyzer();
        var first = analyzer.analyze(fixture, options);
        assertEquals(first, analyzer.analyze(fixture, options));
        assertEquals(AnalysisStatus.SUCCESS, first.status());
        var children = first.root().orElseThrow().children();
        assertEquals(List.of("api", "library"), children.stream().map(MavenModule::baseDirectory).toList());
        assertEquals("api/code/java", children.getFirst().sourceDirectory().resolvedValue().orElseThrow());
        assertEquals(13, children.getFirst().sourceDirectory().origin().startLine().orElseThrow());
        assertTrue(children.get(1).parentCoordinates().isEmpty());
    }

    @Test
    void preservesAggregationSeparateFromInheritanceAndResolvesSources() throws IOException {
        pom("", "<packaging>pom</packaging><modules><module>child</module><module>independent</module></modules>");
        write("child", """
                <project><modelVersion>4.0.0</modelVersion>
                <parent><groupId>example</groupId><artifactId>root</artifactId><version>1</version></parent>
                <artifactId>child</artifactId><properties><code>custom/java</code></properties>
                <build><sourceDirectory>${code}</sourceDirectory></build></project>
                """);
        pom("independent", "<packaging>pom</packaging><modules><module>nested</module></modules>");
        pom("independent/nested", "");
        var analysis = analyze();
        assertEquals(AnalysisStatus.SUCCESS, analysis.status(), analysis.diagnostics().toString());
        MavenModule module = analysis.root().orElseThrow();
        assertEquals(".", module.baseDirectory());
        assertEquals(2, module.children().size());
        MavenModule child = module.children().getFirst();
        assertEquals("example:root:1", child.parentCoordinates().orElseThrow());
        assertEquals("example", child.metadata().groupId().resolvedValue().orElseThrow());
        assertEquals("pom.xml", child.metadata().groupId().origin().sourceFileId());
        assertEquals("${code}", child.sourceDirectory().rawValue().orElseThrow());
        assertEquals("child/custom/java", child.sourceDirectory().resolvedValue().orElseThrow());
        assertEquals("child/pom.xml", child.sourceDirectory().origin().sourceFileId());
        MavenModule independent = module.children().get(1);
        assertTrue(independent.parentCoordinates().isEmpty());
        assertEquals("independent/nested/src/main/java", independent.children().getFirst()
                .sourceDirectory().resolvedValue().orElseThrow());
    }

    @Test
    void normalizesPathsAndContinuesAfterMissingEscapingAndBrokenModules() throws IOException {
        pom("", "<packaging>pom</packaging><modules><module>missing</module><module>../outside</module>"
                + "<module>bad</module><module>ok/../good</module></modules>");
        write("bad", "<project>");
        pom("good", "");
        var analysis = analyze();
        assertEquals(AnalysisStatus.PARTIAL, analysis.status());
        assertEquals(1, analysis.root().orElseThrow().children().size());
        assertEquals("good", analysis.root().orElseThrow().children().getFirst().baseDirectory());
        assertTrue(analysis.diagnostics().stream().anyMatch(d -> d.code().equals("MAVEN_MODULE_PATH_REJECTED")));
        assertTrue(analysis.diagnostics().stream().anyMatch(d -> d.code().equals("MAVEN_POM_XML_INVALID")));
    }

    @Test
    void duplicatesAndCyclesTerminateWithDiagnostics() throws IOException {
        pom("", "<packaging>pom</packaging><modules><module>child</module><module>./child</module></modules>");
        pom("child", "<packaging>pom</packaging><modules><module>..</module></modules>");
        var analysis = analyze();
        assertEquals(AnalysisStatus.PARTIAL, analysis.status());
        assertEquals(1, analysis.root().orElseThrow().children().size());
        assertTrue(analysis.root().orElseThrow().children().getFirst().children().isEmpty());
        assertTrue(analysis.diagnostics().stream().anyMatch(d -> d.code().equals("MAVEN_MODULE_REPEATED")));
    }

    @Test
    void profilesControlModuleDiscovery() throws IOException {
        pom("", """
                <packaging>pom</packaging><profiles><profile><id>extra</id>
                <modules><module>child</module></modules></profile></profiles>
                """);
        pom("child", "");
        assertTrue(analyze().root().orElseThrow().children().isEmpty());
        var analysis = new MavenModuleAnalyzer().analyze(root,
                new MavenModelOptions(root.resolve("cache"), List.of("extra"), 1_048_576));
        assertEquals(AnalysisStatus.SUCCESS, analysis.status(), analysis.diagnostics().toString());
        assertEquals(1, analysis.root().orElseThrow().children().size());
    }

    @Test
    void sourcePathCannotEscapeAndBasedirCanResolve() throws IOException {
        pom("", "<build><sourceDirectory>${project.basedir}/code</sourceDirectory></build>");
        assertEquals("code", analyze().root().orElseThrow().sourceDirectory().resolvedValue().orElseThrow());
        pom("", "<build><sourceDirectory>../outside</sourceDirectory></build>");
        var rejected = analyze();
        assertEquals(AnalysisStatus.PARTIAL, rejected.status());
        assertTrue(rejected.root().orElseThrow().sourceDirectory().resolvedValue().isEmpty());
    }

    @Test
    void inheritedSourceDirectoryIsRelativeToChild() throws IOException {
        pom("", "<packaging>pom</packaging><build><sourceDirectory>code</sourceDirectory></build>"
                + "<modules><module>child</module></modules>");
        write("child", """
                <project><modelVersion>4.0.0</modelVersion><parent><groupId>example</groupId>
                <artifactId>root</artifactId><version>1</version></parent><artifactId>child</artifactId></project>
                """);
        var child = analyze().root().orElseThrow().children().getFirst();
        assertEquals("code", child.sourceDirectory().rawValue().orElseThrow());
        assertEquals("child/code", child.sourceDirectory().resolvedValue().orElseThrow());
        assertEquals("pom.xml", child.sourceDirectory().origin().sourceFileId());
    }

    @Test
    void unreadableRootIsFailed() throws IOException {
        write("", "<project>");
        assertEquals(AnalysisStatus.FAILED, analyze().status());
        assertTrue(analyze().root().isEmpty());
    }

    private MavenModuleAnalysis analyze() {
        return new MavenModuleAnalyzer().analyze(root,
                new MavenModelOptions(root.resolve("cache"), List.of(), 1_048_576));
    }

    private void pom(String directory, String body) throws IOException {
        write(directory, "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>"
                + "<artifactId>root</artifactId><version>1</version>" + body + "</project>");
    }

    private void write(String directory, String xml) throws IOException {
        Path path = root.resolve(directory);
        Files.createDirectories(path);
        Files.writeString(path.resolve("pom.xml"), xml);
    }
}
