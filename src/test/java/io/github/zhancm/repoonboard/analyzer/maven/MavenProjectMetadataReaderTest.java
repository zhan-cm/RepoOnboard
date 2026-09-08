package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenProjectMetadataReaderTest {

    private final MavenProjectMetadataReader reader = new MavenProjectMetadataReader();

    @Test
    void marksMissingFieldsAndProducesDiagnostics(@TempDir Path scanRoot) throws IOException {
        writePom(scanRoot, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <artifactId>incomplete-project</artifactId>
                </project>
                """);

        MavenProjectMetadata metadata = reader.read(scanRoot);

        assertEquals(MavenResolutionStatus.MISSING, metadata.groupId().resolutionStatus());
        assertEquals(MavenResolutionStatus.MISSING, metadata.version().resolutionStatus());
        assertEquals("incomplete-project", metadata.artifactId().resolvedValue().orElseThrow());
        assertEquals("jar", metadata.packaging().resolvedValue().orElseThrow());
        assertEquals(AnalysisStatus.PARTIAL, metadata.status());
        assertTrue(metadata.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("MAVEN_METADATA_MISSING")));
    }

    @Test
    void preservesUnresolvedPropertyExpressions(@TempDir Path scanRoot) throws IOException {
        writePom(scanRoot, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>${project.group}</groupId>
                  <artifactId>property-project</artifactId>
                  <version>1.0.0</version>
                </project>
                """);

        MavenProjectMetadata metadata = reader.read(scanRoot);

        assertEquals(MavenResolutionStatus.UNRESOLVED, metadata.groupId().resolutionStatus());
        assertEquals("${project.group}", metadata.groupId().rawValue().orElseThrow());
        assertTrue(metadata.groupId().resolvedValue().isEmpty());
        assertEquals(AnalysisStatus.PARTIAL, metadata.status());
        assertTrue(metadata.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("MAVEN_METADATA_UNRESOLVED")));
    }

    @Test
    void returnsFailedMetadataForMalformedPom(@TempDir Path scanRoot) throws IOException {
        writePom(scanRoot, "<project><artifactId>broken");

        MavenProjectMetadata metadata = reader.read(scanRoot);

        assertEquals(AnalysisStatus.FAILED, metadata.status());
        assertEquals("MAVEN_POM_XML_INVALID", metadata.diagnostics().get(0).code());
        assertFalse(metadata.groupId().rawValue().isPresent());
        assertEquals(MavenResolutionStatus.MISSING, metadata.packaging().resolutionStatus());
    }

    private static void writePom(Path scanRoot, String contents) throws IOException {
        Files.writeString(scanRoot.resolve("pom.xml"), contents);
    }
}
