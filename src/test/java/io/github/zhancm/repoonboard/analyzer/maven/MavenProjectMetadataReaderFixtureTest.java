package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("fixture")
class MavenProjectMetadataReaderFixtureTest {

    @Test
    void readsBasicMetadataFromSingleModuleProject() {
        MavenProjectMetadata metadata = new MavenProjectMetadataReader()
                .read(FixturePaths.project("minimal-maven-project"));

        assertResolved(metadata.groupId(), "example", "example");
        assertResolved(
                metadata.artifactId(), "minimal-maven-project", "minimal-maven-project");
        assertResolved(metadata.version(), "1.0.0", "1.0.0");
        assertEquals(MavenResolutionStatus.RESOLVED, metadata.packaging().resolutionStatus());
        assertTrue(metadata.packaging().rawValue().isEmpty());
        assertEquals("jar", metadata.packaging().resolvedValue().orElseThrow());
        assertEquals("pom.xml", metadata.groupId().origin().sourceFileId());
        assertTrue(metadata.groupId().origin().startLine().isPresent());
        assertEquals(AnalysisStatus.SUCCESS, metadata.status());
        assertTrue(metadata.diagnostics().isEmpty());
    }

    private static void assertResolved(
            MavenMetadataValue value, String expectedRaw, String expectedResolved) {
        assertEquals(MavenResolutionStatus.RESOLVED, value.resolutionStatus());
        assertEquals(expectedRaw, value.rawValue().orElseThrow());
        assertEquals(expectedResolved, value.resolvedValue().orElseThrow());
    }
}
