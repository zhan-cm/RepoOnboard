package io.github.zhancm.repoonboard.analyzer.maven;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Options that keep Maven model resolution local, bounded, and deterministic. */
public record MavenModelOptions(
        Path localRepository,
        List<String> activeProfileIds,
        long maximumPomBytes) {

    public static final long DEFAULT_MAXIMUM_POM_BYTES = 1_048_576;

    public MavenModelOptions {
        localRepository = Objects.requireNonNull(localRepository, "localRepository")
                .toAbsolutePath()
                .normalize();
        Objects.requireNonNull(activeProfileIds, "activeProfileIds");
        LinkedHashSet<String> uniqueProfiles = new LinkedHashSet<>();
        for (String profileId : activeProfileIds) {
            Objects.requireNonNull(profileId, "activeProfileIds element");
            if (profileId.isBlank()) {
                throw new IllegalArgumentException("active profile IDs must not be blank");
            }
            uniqueProfiles.add(profileId);
        }
        activeProfileIds = List.copyOf(uniqueProfiles);
        if (maximumPomBytes < 1) {
            throw new IllegalArgumentException("maximumPomBytes must be positive");
        }
    }

    /** Uses the standard local Maven repository without reading Maven settings. */
    public static MavenModelOptions defaults() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            throw new IllegalStateException("user.home is required to locate the local Maven repository");
        }
        return new MavenModelOptions(
                Path.of(userHome, ".m2", "repository"),
                List.of(),
                DEFAULT_MAXIMUM_POM_BYTES);
    }
}
