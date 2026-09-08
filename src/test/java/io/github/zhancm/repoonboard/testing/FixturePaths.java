package io.github.zhancm.repoonboard.testing;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FixturePaths {

    private static final String FIXTURE_ROOT = "fixtures/";

    private FixturePaths() {
    }

    public static Path project(String name) {
        validateName(name);

        URL resource = FixturePaths.class.getClassLoader().getResource(FIXTURE_ROOT + name);
        if (resource == null) {
            throw new IllegalArgumentException("Unknown fixture: " + name);
        }

        try {
            Path fixture = Path.of(resource.toURI());
            if (!Files.isDirectory(fixture)) {
                throw new IllegalStateException("Fixture is not a directory: " + name);
            }
            return fixture;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Fixture has an invalid resource URI: " + name, exception);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.contains("..")
                || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Fixture name must be a simple directory name");
        }
    }
}
