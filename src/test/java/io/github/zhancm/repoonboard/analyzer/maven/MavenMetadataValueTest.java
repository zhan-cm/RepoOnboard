package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MavenMetadataValueTest {

    @Test
    void enforcesResolutionStateInvariants() {
        SourceLocation origin = SourceLocation.file("pom.xml");

        assertThrows(IllegalArgumentException.class, () -> new MavenMetadataValue(
                Optional.empty(),
                Optional.empty(),
                MavenResolutionStatus.RESOLVED,
                origin));
        assertThrows(IllegalArgumentException.class, () -> new MavenMetadataValue(
                Optional.empty(),
                Optional.of("value"),
                MavenResolutionStatus.UNRESOLVED,
                origin));
        assertThrows(IllegalArgumentException.class, () -> new MavenMetadataValue(
                Optional.of("value"),
                Optional.empty(),
                MavenResolutionStatus.MISSING,
                origin));
    }
}
