package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MavenProjectDetectionTest {

    @Test
    void enforcesEvidenceConsistency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MavenProjectDetection(true, Optional.empty()));
    }
}
