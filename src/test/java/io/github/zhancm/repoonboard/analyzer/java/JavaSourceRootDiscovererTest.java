package io.github.zhancm.repoonboard.analyzer.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaSourceRootDiscovererTest {

    @TempDir
    Path scanRoot;

    @Test
    void discoversSingleModuleDefaultSourceRoot() throws IOException {
        writePom("", "");
        Files.createDirectories(scanRoot.resolve("src/main/java"));

        JavaSourceRootDiscovery discovery = discover();

        assertEquals(AnalysisStatus.SUCCESS, discovery.status());
        assertEquals(1, discovery.sourceRoots().size());
        JavaSourceRoot root = discovery.sourceRoots().getFirst();
        assertEquals("pom.xml", root.modulePomFileId());
        assertEquals(".", root.moduleBaseDirectory());
        assertEquals("src/main/java", root.relativePath());
        assertEquals("pom.xml", root.declaration().sourceFileId());
    }

    @Test
    void discoversExistingRootsForEachModule() throws IOException {
        writePom("", "<packaging>pom</packaging><modules><module>api</module>"
                + "<module>library</module></modules>");
        writePom("api", "<build><sourceDirectory>code/java</sourceDirectory></build>");
        writePom("library", "");
        Files.createDirectories(scanRoot.resolve("api/code/java"));
        Files.createDirectories(scanRoot.resolve("library/src/main/java"));

        JavaSourceRootDiscovery discovery = discover();

        assertEquals(AnalysisStatus.SUCCESS, discovery.status());
        assertEquals(
                List.of("api/code/java", "library/src/main/java"),
                discovery.sourceRoots().stream().map(JavaSourceRoot::relativePath).toList());
        assertEquals(
                List.of("api/pom.xml", "library/pom.xml"),
                discovery.sourceRoots().stream().map(JavaSourceRoot::modulePomFileId).toList());
    }

    @Test
    void excludesTargetAndGeneratedOutputWithoutScanningThem() throws IOException {
        writePom("", "<build><sourceDirectory>target/generated-sources/code</sourceDirectory></build>");
        Files.createDirectories(scanRoot.resolve("target/generated-sources/code"));
        Files.writeString(scanRoot.resolve("target/generated-sources/code/Generated.java"), "class Generated {}");

        JavaSourceRootDiscovery discovery = discover();

        assertTrue(discovery.sourceRoots().isEmpty());
        assertEquals(AnalysisStatus.PARTIAL, discovery.status());
        assertTrue(discovery.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("JAVA_SOURCE_ROOT_EXCLUDED")));
    }

    @Test
    void ignoresOrdinaryModulesWithoutAnExistingMainSourceRoot() throws IOException {
        writePom("", "");
        Files.createDirectories(scanRoot.resolve("src/test/java"));

        JavaSourceRootDiscovery discovery = discover();

        assertTrue(discovery.sourceRoots().isEmpty());
        assertEquals(AnalysisStatus.SUCCESS, discovery.status());
    }

    private JavaSourceRootDiscovery discover() {
        MavenModelOptions options = new MavenModelOptions(
                scanRoot.resolve("cache"), List.of(), MavenModelOptions.DEFAULT_MAXIMUM_POM_BYTES);
        var maven = new MavenModuleAnalyzer().analyze(scanRoot, options);
        return new JavaSourceRootDiscoverer().discover(scanRoot, maven);
    }

    private void writePom(String module, String body) throws IOException {
        Path directory = scanRoot.resolve(module);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>example</groupId><artifactId>app</artifactId><version>1</version>"
                + body + "</project>");
    }
}
