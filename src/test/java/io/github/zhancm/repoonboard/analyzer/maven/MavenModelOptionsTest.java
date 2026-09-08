package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MavenModelOptionsTest {

    @Test
    void normalizesProfilesAndRejectsInvalidLimits() {
        MavenModelOptions options = new MavenModelOptions(
                Path.of("local-repository"), List.of("dev", "dev", "release"), 128);

        assertEquals(List.of("dev", "release"), options.activeProfileIds());
        assertThrows(
                IllegalArgumentException.class,
                () -> new MavenModelOptions(Path.of("repo"), List.of(" "), 128));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MavenModelOptions(Path.of("repo"), List.of(), 0));
    }
}
