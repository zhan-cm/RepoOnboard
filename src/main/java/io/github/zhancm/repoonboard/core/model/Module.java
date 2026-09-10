package io.github.zhancm.repoonboard.core.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A build module with only analyzer-neutral metadata. */
public record Module(
        String id,
        String pomFileId,
        String baseDirectory,
        Optional<String> aggregationParentModuleId,
        Optional<String> groupId,
        Optional<String> artifactId,
        Optional<String> version,
        Optional<String> packaging,
        List<String> sourceRoots,
        List<Framework> frameworks,
        List<LanguageVersion> languageVersions,
        List<FrameworkVersion> frameworkVersions,
        List<Evidence> evidence) {
    public Module {
        id = ModelValues.requireText(id, "id");
        pomFileId = SourceLocation.file(pomFileId).sourceFileId();
        baseDirectory = ModelValues.requireRelativeDirectory(baseDirectory, "baseDirectory");
        aggregationParentModuleId = ModelValues.requireOptionalText(
                aggregationParentModuleId, "aggregationParentModuleId");
        groupId = ModelValues.requireOptionalText(groupId, "groupId");
        artifactId = ModelValues.requireOptionalText(artifactId, "artifactId");
        version = ModelValues.requireOptionalText(version, "version");
        packaging = ModelValues.requireOptionalText(packaging, "packaging");
        sourceRoots = Objects.requireNonNull(sourceRoots, "sourceRoots").stream()
                .distinct().sorted().toList();
        frameworks = Objects.requireNonNull(frameworks, "frameworks").stream()
                .distinct().sorted().toList();
        languageVersions = Objects.requireNonNull(languageVersions, "languageVersions").stream()
                .distinct()
                .sorted(Comparator.comparing((LanguageVersion value) -> value.language().name())
                        .thenComparing(LanguageVersion::version))
                .toList();
        frameworkVersions = Objects.requireNonNull(frameworkVersions, "frameworkVersions").stream()
                .distinct()
                .sorted(Comparator.comparing((FrameworkVersion value) -> value.framework().name())
                        .thenComparing(FrameworkVersion::version))
                .toList();
        evidence = ModelValues.stableEvidence(evidence);
    }
}
