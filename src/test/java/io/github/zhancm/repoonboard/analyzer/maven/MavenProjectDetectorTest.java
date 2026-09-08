package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenProjectDetectorTest {

    private final MavenProjectDetector detector = new MavenProjectDetector();

    @Test
    void returnsExplicitNonMavenResultForEmptyDirectory(@TempDir Path scanRoot) {
        MavenProjectDetection detection = detector.detect(scanRoot);

        assertFalse(detection.detected());
        assertFalse(detection.evidence().isPresent());
    }

    @Test
    void ignoresUnrelatedFilesAndNestedPom(@TempDir Path scanRoot) throws IOException {
        Files.writeString(scanRoot.resolve("README.md"), "unrelated");
        Path nestedDirectory = Files.createDirectory(scanRoot.resolve("nested"));
        Files.writeString(nestedDirectory.resolve("pom.xml"), "<project/>");

        MavenProjectDetection detection = detector.detect(scanRoot);

        assertFalse(detection.detected());
    }

    @Test
    void doesNotTreatAPomNamedDirectoryAsAProject(@TempDir Path scanRoot) throws IOException {
        Files.createDirectory(scanRoot.resolve("pom.xml"));

        assertFalse(detector.detect(scanRoot).detected());
    }

    @Test
    void rejectsANonDirectoryScanRoot(@TempDir Path temporaryDirectory) throws IOException {
        Path file = Files.createFile(temporaryDirectory.resolve("repository.txt"));

        assertThrows(IllegalArgumentException.class, () -> detector.detect(file));
    }
}
