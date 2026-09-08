package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.model.Activation;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.profile.DefaultProfileSelector;

/** Builds bounded, offline Maven metadata while retaining raw source evidence. */
public final class MavenProjectMetadataReader {

    private static final String STAGE = "MAVEN_MODEL";
    private static final String MAVEN_PROPERTY_PREFIX = "${";
    private static final Pattern PROPERTY_EXPRESSION = Pattern.compile("\\$\\{([^{}]+)}");
    private final String pomFileId;

    public MavenProjectMetadataReader() {
        this("pom.xml");
    }

    private MavenProjectMetadataReader(String pomFileId) {
        this.pomFileId = pomFileId;
    }

    public MavenProjectMetadata read(Path scanRoot) {
        return read(scanRoot, MavenModelOptions.defaults());
    }

    public MavenProjectMetadata read(Path scanRoot, MavenModelOptions options) {
        if (scanRoot == null || !Files.isDirectory(scanRoot)) {
            throw new IllegalArgumentException("scanRoot must be an existing directory");
        }
        Objects.requireNonNull(options, "options");
        if (!Files.isRegularFile(scanRoot.resolve(pomFileId))) {
            throw new IllegalArgumentException("scanRoot must contain a root pom.xml file");
        }

        final RestrictedPomRepository repository;
        final RestrictedPomSource rootSource;
        try {
            repository = new RestrictedPomRepository(scanRoot, options);
            rootSource = repository.rootSource();
        } catch (IOException exception) {
            return failedMetadata(
                    "MAVEN_SCAN_ROOT_UNAVAILABLE",
                    "The Maven scan root could not be resolved safely.");
        } catch (RestrictedPomException exception) {
            return failedMetadata(exception.code(), exception.getMessage());
        }

        return build(repository, rootSource, options, true).metadata();
    }

    static BuiltModel buildSource(RestrictedPomRepository repository, RestrictedPomSource source,
            MavenModelOptions options) {
        return new MavenProjectMetadataReader(source.sourceFileId()).build(repository, source, options, false);
    }

    private BuiltModel build(RestrictedPomRepository repository, RestrictedPomSource rootSource,
            MavenModelOptions options, boolean reportMissingProfiles) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        ModelBuildingResult result = null;
        try {
            result = restrictedBuilder().build(request(rootSource, repository, options));
        } catch (ModelBuildingException exception) {
            result = exception.getResult();
            if (result == null) {
                addDiagnostic(
                        diagnostics,
                        "MAVEN_MODEL_BUILD_FAILED",
                        SourceLocation.file(pomFileId),
                        "Maven could not build a complete effective model from permitted local sources.");
            }
        }
        if (result != null) {
            addModelProblems(diagnostics, result.getProblems(), repository);
        }
        addSourceProblems(diagnostics, repository.problems());

        List<String> activeProfileIds = activeProfileIds(result, repository, options);
        addRestrictedProfileDiagnostics(diagnostics, repository, options);
        if (reportMissingProfiles) {
            addMissingExplicitProfileDiagnostics(diagnostics, repository, options);
        }

        Model rawModel = rootSource.rawModel();
        Model effectiveModel = result == null ? null : result.getEffectiveModel();
        Map<String, MavenMetadataValue> properties = properties(
                rawModel, effectiveModel, repository, activeProfileIds, diagnostics);

        MavenMetadataValue groupId = value(
                "groupId", Model::getGroupId, null, rawModel, effectiveModel,
                repository, properties, diagnostics);
        MavenMetadataValue artifactId = value(
                "artifactId", Model::getArtifactId, null, rawModel, effectiveModel,
                repository, properties, diagnostics);
        MavenMetadataValue version = value(
                "version", Model::getVersion, null, rawModel, effectiveModel,
                repository, properties, diagnostics);
        MavenMetadataValue packaging = value(
                "packaging", Model::getPackaging, "jar", rawModel, effectiveModel,
                repository, properties, diagnostics);

        List<String> sourcePomIds = repository.sources().stream()
                .map(RestrictedPomSource::sourceFileId)
                .toList();
        MavenProjectMetadata metadata = new MavenProjectMetadata(
                groupId,
                artifactId,
                version,
                packaging,
                properties,
                activeProfileIds,
                sourcePomIds,
                diagnostics);
        return new BuiltModel(metadata, rawModel, effectiveModel);
    }

    record BuiltModel(MavenProjectMetadata metadata, Model raw, Model effective) { }

    private DefaultModelBuildingRequest request(
            RestrictedPomSource rootSource,
            RestrictedPomRepository repository,
            MavenModelOptions options) {
        return new DefaultModelBuildingRequest()
                .setModelSource(rootSource)
                .setModelResolver(new RestrictedModelResolver(repository))
                .setSystemProperties(new Properties())
                .setUserProperties(new Properties())
                .setActiveProfileIds(options.activeProfileIds())
                .setInactiveProfileIds(List.of())
                .setProcessPlugins(false)
                .setLocationTracking(true)
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);
    }

    private DefaultModelBuilder restrictedBuilder() {
        DefaultModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        builder.setProfileSelector(new DefaultProfileSelector());
        return builder;
    }

    private MavenMetadataValue value(
            String field,
            Function<Model, String> getter,
            String defaultValue,
            Model rawRoot,
            Model effective,
            RestrictedPomRepository repository,
            Map<String, MavenMetadataValue> properties,
            List<Diagnostic> diagnostics) {
        InputLocation effectiveLocation = effective == null ? null : effective.getLocation(field);
        InputLocation rootLocation = rawRoot.getLocation(field);
        RawValue declaration = declaration(
                field, getter, rootLocation, effectiveLocation, rawRoot, repository);
        Optional<String> resolved = optionalText(effective == null ? null : getter.apply(effective));
        if (resolved.isEmpty() && declaration.value().isPresent()) {
            resolved = interpolate(
                    declaration.value().orElseThrow(),
                    propertyStrings(properties),
                    new LinkedHashSet<>());
        }
        if (resolved.isEmpty() && declaration.value().isEmpty() && defaultValue != null) {
            resolved = Optional.of(defaultValue);
        }

        SourceLocation origin = sourceLocation(
                field,
                declaration.location() != null ? declaration.location() : effectiveLocation,
                declaration.sourceFileId(),
                repository);
        if (resolved.isPresent() && !resolved.orElseThrow().contains(MAVEN_PROPERTY_PREFIX)) {
            return new MavenMetadataValue(
                    declaration.value(), resolved, MavenResolutionStatus.RESOLVED, origin);
        }
        if (declaration.value().isPresent()) {
            addDiagnostic(
                    diagnostics,
                    "MAVEN_METADATA_UNRESOLVED",
                    origin,
                    "Maven metadata field could not be fully resolved: " + field);
            return new MavenMetadataValue(
                    declaration.value(), Optional.empty(), MavenResolutionStatus.UNRESOLVED, origin);
        }

        addDiagnostic(
                diagnostics,
                "MAVEN_METADATA_MISSING",
                origin,
                "Maven metadata field is missing: " + field);
        return missingValue(origin);
    }

    private RawValue declaration(
            String field,
            Function<Model, String> getter,
            InputLocation rootLocation,
            InputLocation effectiveLocation,
            Model rawRoot,
            RestrictedPomRepository repository) {
        if (rootLocation != null) {
            return new RawValue(
                    optionalText(getter.apply(rawRoot)), rootLocation, pomFileId);
        }

        String sourceFileId = sourceFileId(effectiveLocation, pomFileId, repository);
        Optional<RestrictedPomSource> source = repository.sourceById(sourceFileId);
        if (source.isPresent()) {
            Model rawSourceModel = source.orElseThrow().rawModel();
            InputLocation rawSourceLocation = rawSourceModel.getLocation(field);
            return new RawValue(
                    rawSourceLocation == null
                            ? Optional.empty()
                            : optionalText(getter.apply(rawSourceModel)),
                    rawSourceLocation,
                    sourceFileId);
        }
        return new RawValue(Optional.empty(), effectiveLocation, pomFileId);
    }

    private Map<String, MavenMetadataValue> properties(
            Model rawRoot,
            Model effective,
            RestrictedPomRepository repository,
            List<String> activeProfileIds,
            List<Diagnostic> diagnostics) {
        Map<String, RawProperty> rawProperties = new LinkedHashMap<>();
        // A BOM or a rejected relative parent is a read source, not an inheritance source.
        // Use the effective property's own location to recover its original declaration.
        collectRawProperties(rawProperties, repository.sourceById(pomFileId).orElseThrow(),
                activeProfileIds);
        Set<String> names = new TreeSet<>();
        if (effective != null) {
            names.addAll(effective.getProperties().stringPropertyNames());
        } else {
            names.addAll(rawProperties.keySet());
        }

        Map<String, MavenMetadataValue> resolvedProperties = new LinkedHashMap<>();
        Map<String, String> fallbackProperties = rawPropertyStrings(rawProperties);
        InputLocation effectivePropertiesLocation = effective == null
                ? null
                : effective.getLocation("properties");
        for (String name : names) {
            RawProperty raw = rawProperties.get(name);
            if (effective != null) {
                InputLocation declared = nestedLocation(effectivePropertiesLocation, name);
                String sourceId = sourceFileId(declared, pomFileId, repository);
                Model sourceModel = repository.sourceById(sourceId).orElseThrow().rawModel();
                String original = sourceModel.getProperties().getProperty(name);
                for (Profile profile : sourceModel.getProfiles()) {
                    InputLocation candidate = nestedLocation(profile.getLocation("properties"), name);
                    if (candidate != null && declared != null
                            && candidate.getLineNumber() == declared.getLineNumber()
                            && candidate.getColumnNumber() == declared.getColumnNumber()) {
                        original = profile.getProperties().getProperty(name);
                    }
                }
                raw = original == null ? null : new RawProperty(original, declared, sourceId);
            }
            Optional<String> rawValue = raw == null ? Optional.empty() : Optional.of(raw.value());
            Optional<String> resolvedValue = optionalText(
                    effective == null ? null : effective.getProperties().getProperty(name));
            if (resolvedValue.isEmpty() && rawValue.isPresent()) {
                resolvedValue = interpolate(
                        rawValue.orElseThrow(), fallbackProperties, new LinkedHashSet<>());
            }
            InputLocation location = raw == null ? null : raw.location();
            if (location == null && effectivePropertiesLocation != null) {
                location = effectivePropertiesLocation.getLocation(name);
            }
            String sourceFileId = raw == null ? pomFileId : raw.sourceFileId();
            SourceLocation origin = sourceLocation(
                    "properties." + name, location, sourceFileId, repository);
            if (resolvedValue.isPresent()
                    && !resolvedValue.orElseThrow().contains(MAVEN_PROPERTY_PREFIX)) {
                resolvedProperties.put(name, new MavenMetadataValue(
                        rawValue,
                        resolvedValue,
                        MavenResolutionStatus.RESOLVED,
                        origin));
            } else if (rawValue.isPresent()) {
                addDiagnostic(
                        diagnostics,
                        "MAVEN_PROPERTY_UNRESOLVED",
                        origin,
                        "Maven property could not be fully resolved: " + name);
                resolvedProperties.put(name, new MavenMetadataValue(
                        rawValue,
                        Optional.empty(),
                        MavenResolutionStatus.UNRESOLVED,
                        origin));
            }
        }
        return Collections.unmodifiableMap(resolvedProperties);
    }

    private void collectRawProperties(
            Map<String, RawProperty> destination,
            RestrictedPomSource source,
            List<String> activeProfileIds) {
        Model raw = source.rawModel();
        Map<String, RawProperty> sourceProperties = new LinkedHashMap<>();
        InputLocation modelProperties = raw.getLocation("properties");
        for (String name : raw.getProperties().stringPropertyNames()) {
            sourceProperties.put(name, new RawProperty(
                    raw.getProperties().getProperty(name),
                    nestedLocation(modelProperties, name),
                    source.sourceFileId()));
        }

        for (Profile profile : raw.getProfiles()) {
            if (!activeProfileIds.contains(profile.getId())) {
                continue;
            }
            InputLocation profileProperties = profile.getLocation("properties");
            for (String name : profile.getProperties().stringPropertyNames()) {
                sourceProperties.put(name, new RawProperty(
                        profile.getProperties().getProperty(name),
                        nestedLocation(profileProperties, name),
                        source.sourceFileId()));
            }
        }
        sourceProperties.forEach(destination::putIfAbsent);
    }

    private List<String> activeProfileIds(
            ModelBuildingResult result,
            RestrictedPomRepository repository,
            MavenModelOptions options) {
        LinkedHashSet<String> active = new LinkedHashSet<>();
        if (result != null) {
            for (String modelId : result.getModelIds()) {
                List<Profile> profiles = result.getActivePomProfiles(modelId);
                if (profiles != null) {
                    for (Profile profile : profiles) {
                        active.add(profile.getId());
                    }
                }
            }
        }
        if (result == null || result.getEffectiveModel() == null) {
            active.clear();
            List<Profile> profiles = repository.sourceById(pomFileId).orElseThrow()
                    .rawModel().getProfiles();
            boolean explicit = profiles.stream()
                    .anyMatch(profile -> options.activeProfileIds().contains(profile.getId()));
            for (Profile profile : profiles) {
                if (options.activeProfileIds().contains(profile.getId())
                        || (!explicit && isActiveByDefaultOnly(profile))) {
                    active.add(profile.getId());
                }
            }
        }
        return List.copyOf(active);
    }

    private void addRestrictedProfileDiagnostics(
            List<Diagnostic> diagnostics,
            RestrictedPomRepository repository,
            MavenModelOptions options) {
        for (RestrictedPomSource source : repository.sources()) {
            for (Profile profile : source.rawModel().getProfiles()) {
                if (options.activeProfileIds().contains(profile.getId())) {
                    continue;
                }
                Activation activation = profile.getActivation();
                if (activation == null || !hasImplicitActivation(activation)) {
                    continue;
                }
                SourceLocation origin = sourceLocation(
                        "profiles." + profile.getId() + ".activation",
                        profile.getLocation("activation"),
                        source.sourceFileId(),
                        repository);
                addDiagnostic(
                        diagnostics,
                        "MAVEN_PROFILE_ACTIVATION_IGNORED",
                        origin,
                        "Host-dependent activation was ignored for Maven profile: "
                                + profile.getId());
            }
        }
    }

    private void addMissingExplicitProfileDiagnostics(
            List<Diagnostic> diagnostics,
            RestrictedPomRepository repository,
            MavenModelOptions options) {
        Set<String> declaredIds = new LinkedHashSet<>();
        for (RestrictedPomSource source : repository.sources()) {
            source.rawModel().getProfiles().stream()
                    .map(Profile::getId)
                    .filter(Objects::nonNull)
                    .forEach(declaredIds::add);
        }
        for (String requested : options.activeProfileIds()) {
            if (!declaredIds.contains(requested)) {
                addDiagnostic(
                        diagnostics,
                        "MAVEN_PROFILE_NOT_FOUND",
                        SourceLocation.file(pomFileId),
                        "An explicitly requested Maven profile was not found: " + requested);
            }
        }
    }

    private void addSourceProblems(
            List<Diagnostic> diagnostics, List<PomSourceProblem> problems) {
        for (PomSourceProblem problem : problems) {
            addDiagnostic(
                    diagnostics,
                    problem.code(),
                    SourceLocation.file(problem.sourceFileId()),
                    problem.message());
        }
    }

    private void addModelProblems(
            List<Diagnostic> diagnostics,
            List<ModelProblem> problems,
            RestrictedPomRepository repository) {
        for (ModelProblem problem : problems) {
            String sourceFileId = problem.getSource();
            if (sourceFileId == null || !repository.recognizesSource(sourceFileId)) {
                sourceFileId = pomFileId;
            }
            SourceLocation location = new SourceLocation(
                    sourceFileId,
                    positive(problem.getLineNumber()),
                    positive(problem.getColumnNumber()),
                    OptionalInt.empty(),
                    OptionalInt.empty(),
                    Optional.empty());
            addDiagnostic(
                    diagnostics,
                    "MAVEN_MODEL_PROBLEM",
                    location,
                    "Maven could not build a complete effective model from permitted local sources.");
        }
    }

    private boolean hasImplicitActivation(Activation activation) {
        return activation.getJdk() != null
                || activation.getOs() != null
                || activation.getProperty() != null
                || activation.getFile() != null;
    }

    private boolean isActiveByDefaultOnly(Profile profile) {
        Activation activation = profile.getActivation();
        return activation != null
                && activation.isActiveByDefault()
                && !hasImplicitActivation(activation);
    }

    private Optional<String> interpolate(
            String value, Map<String, String> properties, Set<String> visiting) {
        if (visiting.size() > 64 || value.length() > 65_536) {
            return Optional.empty();
        }
        Matcher matcher = PROPERTY_EXPRESSION.matcher(value);
        StringBuilder resolved = new StringBuilder();
        int previousEnd = 0;
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = properties.get(name);
            if (replacement == null || !visiting.add(name)) {
                return Optional.empty();
            }
            Optional<String> nested = interpolate(replacement, properties, visiting);
            visiting.remove(name);
            if (nested.isEmpty()) {
                return Optional.empty();
            }
            resolved.append(value, previousEnd, matcher.start());
            resolved.append(nested.orElseThrow());
            if (resolved.length() > 65_536) {
                return Optional.empty();
            }
            previousEnd = matcher.end();
        }
        resolved.append(value, previousEnd, value.length());
        String result = resolved.toString();
        return result.contains(MAVEN_PROPERTY_PREFIX) ? Optional.empty() : Optional.of(result);
    }

    private Map<String, String> propertyStrings(
            Map<String, MavenMetadataValue> properties) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, MavenMetadataValue> entry : properties.entrySet()) {
            MavenMetadataValue property = entry.getValue();
            property.resolvedValue()
                    .or(() -> property.rawValue())
                    .ifPresent(value -> values.put(entry.getKey(), value));
        }
        return values;
    }

    private Map<String, String> rawPropertyStrings(
            Map<String, RawProperty> properties) {
        Map<String, String> values = new LinkedHashMap<>();
        properties.forEach((name, property) -> values.put(name, property.value()));
        return values;
    }

    private InputLocation nestedLocation(InputLocation parent, String key) {
        return parent == null ? null : parent.getLocation(key);
    }

    private SourceLocation sourceLocation(
            String symbol,
            InputLocation inputLocation,
            String fallbackSourceFileId,
            RestrictedPomRepository repository) {
        String sourceFileId = sourceFileId(inputLocation, fallbackSourceFileId, repository);
        return new SourceLocation(
                sourceFileId,
                positive(inputLocation == null ? 0 : inputLocation.getLineNumber()),
                positive(inputLocation == null ? 0 : inputLocation.getColumnNumber()),
                OptionalInt.empty(),
                OptionalInt.empty(),
                Optional.of("project." + symbol));
    }

    private String sourceFileId(
            InputLocation inputLocation,
            String fallback,
            RestrictedPomRepository repository) {
        if (inputLocation != null && inputLocation.getSource() != null) {
            String location = inputLocation.getSource().getLocation();
            if (location != null && repository.recognizesSource(location)) {
                return location;
            }
        }
        return fallback;
    }

    private MavenProjectMetadata failedMetadata(String code, String message) {
        SourceLocation pomLocation = SourceLocation.file(pomFileId);
        Diagnostic diagnostic = new Diagnostic(
                code,
                DiagnosticSeverity.ERROR,
                STAGE,
                Optional.empty(),
                Optional.of(pomFileId),
                Optional.of(pomLocation),
                message);
        MavenMetadataValue unavailable = missingValue(pomLocation);
        return new MavenProjectMetadata(
                unavailable,
                unavailable,
                unavailable,
                unavailable,
                Map.of(),
                List.of(),
                List.of(pomFileId),
                List.of(diagnostic));
    }

    private MavenMetadataValue missingValue(SourceLocation origin) {
        return new MavenMetadataValue(
                Optional.empty(),
                Optional.empty(),
                MavenResolutionStatus.MISSING,
                origin);
    }

    private void addDiagnostic(
            List<Diagnostic> diagnostics,
            String code,
            SourceLocation location,
            String message) {
        Diagnostic diagnostic = new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.empty(),
                Optional.of(location.sourceFileId()),
                Optional.of(location),
                message);
        if (!diagnostics.contains(diagnostic)) {
            diagnostics.add(diagnostic);
        }
    }

    private OptionalInt positive(int value) {
        return value > 0 ? OptionalInt.of(value) : OptionalInt.empty();
    }

    private Optional<String> optionalText(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private record RawValue(
            Optional<String> value, InputLocation location, String sourceFileId) {
    }

    private record RawProperty(String value, InputLocation location, String sourceFileId) {
    }
}
