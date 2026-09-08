package io.github.zhancm.repoonboard.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("fixture")
class MinimalMavenProjectFixtureTest {

    @Test
    void fixtureHasAControlledMavenProjectLayout() throws IOException {
        Path fixture = FixturePaths.project("minimal-maven-project");

        List<String> files;
        try (var paths = Files.walk(fixture)) {
            files = paths
                    .filter(Files::isRegularFile)
                    .map(fixture::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .sorted()
                    .toList();
        }

        assertEquals(List.of("pom.xml", "src/main/java/example/App.java"), files);
        assertTrue(Files.readString(fixture.resolve("pom.xml"))
                .contains("<artifactId>minimal-maven-project</artifactId>"));
        assertTrue(Files.readString(fixture.resolve("src/main/java/example/App.java"))
                .contains("package example;"));
    }
}
