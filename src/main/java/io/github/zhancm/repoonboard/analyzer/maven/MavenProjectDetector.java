package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Detects whether a validated scan root is a Maven project. */
public final class MavenProjectDetector {

    static final String POM_FILE_NAME = "pom.xml";

    /**
     * Detects a Maven project from a root-level {@code pom.xml} regular file.
     *
     * <p>This step intentionally does not read or parse the POM. Model parsing,
     * restricted source access, modules, and metadata belong to later Maven
     * analysis tasks.</p>
     */
    public MavenProjectDetection detect(Path scanRoot) {
        Objects.requireNonNull(scanRoot, "scanRoot");
        if (!Files.isDirectory(scanRoot)) {
            throw new IllegalArgumentException("scanRoot must be an existing directory");
        }

        if (!Files.isRegularFile(scanRoot.resolve(POM_FILE_NAME))) {
            return MavenProjectDetection.notDetected();
        }

        Evidence evidence = new Evidence(
                "BUILD_METADATA",
                SourceLocation.file(POM_FILE_NAME),
                List.of(),
                "maven.root-pom-present");
        return MavenProjectDetection.detected(evidence);
    }
}
