package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;

final class MavenDependencyExtractor {
    List<MavenDependency> extract(MavenProjectMetadataReader.BuiltModel built,
            RestrictedPomSource source, RestrictedPomRepository repository, List<Diagnostic> diagnostics) {
        boolean effective = built.effective() != null;
        List<Dependency> dependencies = new ArrayList<>(
                (effective ? built.effective() : built.raw()).getDependencies());
        if (!effective) {
            built.raw().getProfiles().stream()
                    .filter(profile -> built.metadata().activeProfileIds().contains(profile.getId()))
                    .forEach(profile -> dependencies.addAll(profile.getDependencies()));
        }
        List<MavenDependency> results = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            results.add(new MavenDependency(
                    field("groupId", Dependency::getGroupId, dependency, built, source, repository, diagnostics),
                    field("artifactId", Dependency::getArtifactId, dependency, built, source, repository, diagnostics),
                    field("version", Dependency::getVersion, dependency, built, source, repository, diagnostics),
                    field("scope", Dependency::getScope, dependency, built, source, repository, diagnostics)));
        }
        return List.copyOf(results);
    }

    private MavenMetadataValue field(String field, Function<Dependency, String> getter,
            Dependency dependency, MavenProjectMetadataReader.BuiltModel built,
            RestrictedPomSource source, RestrictedPomRepository repository, List<Diagnostic> diagnostics) {
        InputLocation location = dependency.getLocation(field);
        String sourceId = source.sourceFileId();
        if (location != null && location.getSource() != null
                && repository.recognizesSource(location.getSource().getLocation())) {
            sourceId = location.getSource().getLocation();
        }
        Optional<String> raw = built.effective() == null ? text(getter.apply(dependency))
                : original(field, getter, location, repository.sourceById(sourceId).orElseThrow());
        Optional<String> resolved = text(getter.apply(dependency));
        if (resolved.isPresent() && resolved.get().contains("${")) {
            String value = resolved.get();
            for (var entry : built.metadata().properties().entrySet()) {
                if (entry.getValue().resolvedValue().isPresent()) {
                    value = value.replace("${" + entry.getKey() + "}", entry.getValue().resolvedValue().get());
                }
            }
            resolved = value.contains("${") ? Optional.empty() : text(value);
        }
        if (field.equals("scope") && raw.isEmpty() && resolved.isEmpty()) {
            resolved = Optional.of("compile");
        }
        SourceLocation origin = new SourceLocation(sourceId,
                positive(location == null ? 0 : location.getLineNumber()),
                positive(location == null ? 0 : location.getColumnNumber()), OptionalInt.empty(),
                OptionalInt.empty(), Optional.of("project.dependencies.dependency." + field));
        if (resolved.isPresent()) {
            return new MavenMetadataValue(raw, resolved, MavenResolutionStatus.RESOLVED, origin);
        }
        diagnostics.add(new Diagnostic("MAVEN_DEPENDENCY_FIELD_UNRESOLVED", DiagnosticSeverity.WARNING,
                "MAVEN_DEPENDENCIES", Optional.empty(), Optional.of(sourceId), Optional.of(origin),
                "A dependency field is missing or unresolved: " + field));
        return new MavenMetadataValue(raw, Optional.empty(),
                raw.isPresent() ? MavenResolutionStatus.UNRESOLVED : MavenResolutionStatus.MISSING, origin);
    }

    private Optional<String> original(String field, Function<Dependency, String> getter,
            InputLocation location, RestrictedPomSource source) {
        if (location == null) {
            return Optional.empty();
        }
        var model = source.rawModel();
        List<Dependency> declarations = new ArrayList<>(model.getDependencies());
        if (model.getDependencyManagement() != null) {
            declarations.addAll(model.getDependencyManagement().getDependencies());
        }
        for (var profile : model.getProfiles()) {
            declarations.addAll(profile.getDependencies());
            if (profile.getDependencyManagement() != null) {
                declarations.addAll(profile.getDependencyManagement().getDependencies());
            }
        }
        for (Dependency declaration : declarations) {
            InputLocation candidate = declaration.getLocation(field);
            if (candidate != null && candidate.getLineNumber() == location.getLineNumber()
                    && candidate.getColumnNumber() == location.getColumnNumber()) {
                return text(getter.apply(declaration));
            }
        }
        return Optional.empty();
    }

    private static Optional<String> text(String value) {
        return Optional.ofNullable(value).filter(text -> !text.isBlank());
    }

    private static OptionalInt positive(int number) {
        return number > 0 ? OptionalInt.of(number) : OptionalInt.empty();
    }
}
