package io.github.zhancm.repoonboard.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FixturePathsTest {

    @Test
    void resolvesFixtureDirectoryFromTestResources() {
        Path fixture = FixturePaths.project("minimal-maven-project");

        assertTrue(fixture.isAbsolute());
        assertEquals("minimal-maven-project", fixture.getFileName().toString());
    }

    @Test
    void rejectsUnknownFixture() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FixturePaths.project("missing-project"));

        assertEquals("Unknown fixture: missing-project", exception.getMessage());
    }

    @Test
    void rejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class, () -> FixturePaths.project("../outside"));
    }
}
