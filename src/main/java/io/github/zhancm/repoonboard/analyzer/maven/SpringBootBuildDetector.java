package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;

final class SpringBootBuildDetector {
    SpringBootBuild detect(MavenProjectMetadataReader.BuiltModel built, RestrictedPomSource source,
            List<MavenDependency> dependencies) {
        List<SpringBootBuild.Signal> signals = new ArrayList<>();
        var parent = built.raw().getParent();
        if (parent != null
                && "org.springframework.boot".equals(
                        resolve(parent.getGroupId(), built).orElse(""))
                && "spring-boot-starter-parent".equals(
                        resolve(parent.getArtifactId(), built).orElse(""))) {
            signals.add(new SpringBootBuild.Signal(SpringBootBuildEvidenceKind.PARENT,
                    "spring-boot-starter-parent",
                    resolve(parent.getVersion(), built), location(source, parent.getLocation("artifactId")),
                    location(source, parent.getLocation("version"))));
        }
        List<Dependency> managed = new ArrayList<>();
        if (built.raw().getDependencyManagement() != null) {
            managed.addAll(built.raw().getDependencyManagement().getDependencies());
        }
        for (var profile : built.raw().getProfiles()) {
            if (built.metadata().activeProfileIds().contains(profile.getId())
                    && profile.getDependencyManagement() != null) {
                managed.addAll(profile.getDependencyManagement().getDependencies());
            }
        }
        for (Dependency dependency : managed) {
            if ("org.springframework.boot".equals(resolve(dependency.getGroupId(), built).orElse(""))
                    && "spring-boot-dependencies".equals(resolve(dependency.getArtifactId(), built).orElse(""))
                    && "pom".equals(resolve(dependency.getType(), built).orElse(""))
                    && "import".equals(resolve(dependency.getScope(), built).orElse(""))) {
                signals.add(new SpringBootBuild.Signal(SpringBootBuildEvidenceKind.BOM,
                        "spring-boot-dependencies",
                        resolve(dependency.getVersion(), built),
                        location(source, dependency.getLocation("artifactId")),
                        location(source, dependency.getLocation("version"))));
            }
        }
        for (MavenDependency dependency : dependencies) {
            String artifact = dependency.artifactId().resolvedValue().orElse("");
            if (dependency.groupId().resolvedValue().orElse("").equals("org.springframework.boot")
                    && (artifact.equals("spring-boot") || artifact.equals("spring-boot-autoconfigure")
                    || artifact.equals("spring-boot-starter") || artifact.startsWith("spring-boot-starter-"))) {
                signals.add(new SpringBootBuild.Signal(SpringBootBuildEvidenceKind.DEPENDENCY, artifact,
                        dependency.version().resolvedValue(), dependency.artifactId().origin(),
                        dependency.version().origin()));
            }
        }
        return new SpringBootBuild(signals);
    }

    private Optional<String> resolve(String raw, MavenProjectMetadataReader.BuiltModel built) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String resolved = raw;
        for (var entry : built.metadata().properties().entrySet()) {
            if (entry.getValue().resolvedValue().isPresent()) {
                resolved = resolved.replace("${" + entry.getKey() + "}", entry.getValue().resolvedValue().get());
            }
        }
        return resolved.contains("${") ? Optional.empty() : Optional.of(resolved);
    }

    private SourceLocation location(RestrictedPomSource source, InputLocation location) {
        return new SourceLocation(source.sourceFileId(),
                location == null || location.getLineNumber() < 1
                        ? OptionalInt.empty() : OptionalInt.of(location.getLineNumber()),
                location == null || location.getColumnNumber() < 1
                        ? OptionalInt.empty() : OptionalInt.of(location.getColumnNumber()),
                OptionalInt.empty(), OptionalInt.empty(), Optional.of("project.springBootBuild"));
    }
}
