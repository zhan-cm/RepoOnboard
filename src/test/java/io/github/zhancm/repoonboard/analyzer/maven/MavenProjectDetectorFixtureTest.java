package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("fixture")
class MavenProjectDetectorFixtureTest {

    @Test
    void detectsRootPomWithTraceableEvidence() {
        MavenProjectDetection detection = new MavenProjectDetector()
                .detect(FixturePaths.project("minimal-maven-project"));

        assertTrue(detection.detected());
        Evidence evidence = detection.evidence().orElseThrow();
        assertEquals("BUILD_METADATA", evidence.type());
        assertEquals("pom.xml", evidence.location().sourceFileId());
        assertEquals("maven.root-pom-present", evidence.ruleId());
    }
}
