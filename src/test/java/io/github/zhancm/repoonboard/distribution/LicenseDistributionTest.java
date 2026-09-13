package io.github.zhancm.repoonboard.distribution;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LicenseDistributionTest {
    private final Path repository = Path.of(System.getProperty("user.dir"));

    @Test
    void repositoryDocumentsProjectAndDistributedComponentLicenses() throws IOException {
        String license = Files.readString(repository.resolve("LICENSE"));
        String notice = Files.readString(repository.resolve("NOTICE"));
        String thirdParty = Files.readString(repository.resolve("THIRD_PARTY_NOTICES.md"));
        String readme = Files.readString(repository.resolve("README.md"));
        String chineseReadme = Files.readString(repository.resolve("README.zh-CN.md"));

        assertAll(
                () -> assertTrue(license.contains("Apache License")),
                () -> assertTrue(license.contains("Copyright 2026 zhan-cm")),
                () -> assertTrue(notice.contains("THIRD_PARTY_NOTICES.md")),
                () -> assertTrue(thirdParty.contains("org.eclipse.sisu:org.eclipse.sisu.inject")),
                () -> assertTrue(thirdParty.contains("com.github.javaparser:javaparser-core")),
                () -> assertTrue(thirdParty.contains("cytoscape")),
                () -> assertTrue(thirdParty.contains("source-map-js")),
                () -> assertTrue(thirdParty.contains("frontend/package-lock.json")),
                () -> assertTrue(readme.contains("licensed under the Apache License 2.0")),
                () -> assertTrue(chineseReadme.contains("Apache License 2.0")));
    }
}
