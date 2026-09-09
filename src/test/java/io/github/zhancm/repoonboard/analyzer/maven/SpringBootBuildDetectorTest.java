package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringBootBuildDetectorTest {
    @TempDir Path root;

    @Test
    void missingBootParentStillProvidesLocatedBuildEvidence() throws Exception {
        var result = analyze("""
                <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.5</version><relativePath/></parent>
                """);
        assertTrue(result.detected());
        assertEquals("3.5.5", result.version().orElseThrow());
        assertEquals(SpringBootBuildEvidenceKind.PARENT, result.evidence().getFirst().kind());
        assertEquals("pom.xml", result.evidence().getFirst().origin().sourceFileId());
        assertTrue(result.evidence().getFirst().origin().startLine().isPresent());
    }

    @Test
    void importedBomPropertyIsBuildEvidenceEvenWithoutCachedBom() throws Exception {
        var result = analyze("""
                <properties><boot.version>3.5.5</boot.version></properties>
                <dependencyManagement><dependencies><dependency><groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId><version>${boot.version}</version>
                <type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>
                """);
        assertTrue(result.detected());
        assertEquals("3.5.5", result.version().orElseThrow());
        assertEquals(SpringBootBuildEvidenceKind.BOM, result.evidence().getFirst().kind());
        assertEquals("pom.xml", result.evidence().getFirst().versionOrigin().sourceFileId());
        assertTrue(result.evidence().getFirst().versionOrigin().startLine().isPresent());
    }

    @Test
    void starterDependencyResolvesVersionFromProperty() throws Exception {
        var result = analyze("<properties><boot.version>3.5.5</boot.version></properties>"
                + "<dependencies>" + dependency("org.springframework.boot", "spring-boot-starter-web", "${boot.version}")
                + "</dependencies>");
        assertTrue(result.detected());
        assertEquals("3.5.5", result.version().orElseThrow());
        assertEquals(SpringBootBuildEvidenceKind.DEPENDENCY, result.evidence().getFirst().kind());
    }

    @Test
    void detectionAndVersionResolutionAreIndependentAndConflictsAreNotGuessed() throws Exception {
        var unresolved = analyze("<dependencies>"
                + dependency("org.springframework.boot", "spring-boot", "${unknown}") + "</dependencies>");
        assertTrue(unresolved.detected());
        assertTrue(unresolved.version().isEmpty());
        var conflicting = analyze("<dependencies>"
                + dependency("org.springframework.boot", "spring-boot", "3.5.5")
                + dependency("org.springframework.boot", "spring-boot-starter-web", "3.4.9") + "</dependencies>");
        assertTrue(conflicting.detected());
        assertTrue(conflicting.version().isEmpty());
        assertEquals(2, conflicting.evidence().size());
    }

    @Test
    void springFrameworkLookalikesAndInactiveProfilesAreNotBootEvidence() throws Exception {
        var result = analyze("<dependencies>"
                + dependency("org.springframework", "spring-context", "6.2.10")
                + dependency("example", "spring-boot-starter-web", "1") + "</dependencies>"
                + "<profiles><profile><id>inactive</id><dependencies>"
                + dependency("org.springframework.boot", "spring-boot", "3.5.5")
                + "</dependencies></profile></profiles>");
        assertFalse(result.detected());
        assertTrue(result.evidence().isEmpty());
    }

    private SpringBootBuild analyze(String body) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>example</groupId><artifactId>app</artifactId><version>1</version>" + body + "</project>");
        return new MavenModuleAnalyzer().analyze(root,
                new MavenModelOptions(root.resolve("empty-cache"), List.of(), 1_048_576))
                .root().orElseThrow().springBoot();
    }

    private String dependency(String group, String artifact, String version) {
        return "<dependency><groupId>" + group + "</groupId><artifactId>" + artifact
                + "</artifactId><version>" + version + "</version></dependency>";
    }
}
