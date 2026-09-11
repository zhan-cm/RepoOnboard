package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.analyzer.maven.MavenDependency;
import io.github.zhancm.repoonboard.analyzer.maven.MavenMetadataValue;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEntryPointFact;
import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.BuildSystem;
import io.github.zhancm.repoonboard.core.model.Component;
import io.github.zhancm.repoonboard.core.model.ComponentKind;
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.Endpoint;
import io.github.zhancm.repoonboard.core.model.EndpointConditions;
import io.github.zhancm.repoonboard.core.model.EntryPoint;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.Framework;
import io.github.zhancm.repoonboard.core.model.FrameworkVersion;
import io.github.zhancm.repoonboard.core.model.Language;
import io.github.zhancm.repoonboard.core.model.LanguageVersion;
import io.github.zhancm.repoonboard.core.model.Project;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import io.github.zhancm.repoonboard.core.model.SourceFile;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import io.github.zhancm.repoonboard.core.model.StableIdentifiers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Maps analyzer-specific facts to the stable, analyzer-neutral public model. */
public final class ProjectModelAssembler {
    public AnalysisReport assemble(ProjectAnalysisInput input) {
        Objects.requireNonNull(input, "input");
        MavenModule root = input.maven().root().orElseThrow(
                () -> new IllegalArgumentException("Maven root module is required"));
        List<MavenModule> mavenModules = flatten(root);
        Map<String, String> moduleIds = mavenModules.stream().collect(java.util.stream.Collectors.toMap(
                MavenModule::pomFileId, module -> moduleId(module.pomFileId()), (left, right) -> left,
                LinkedHashMap::new));
        Map<String, String> aggregationParents = aggregationParents(root);

        List<io.github.zhancm.repoonboard.core.model.Module> modules = mavenModules.stream()
                .map(module -> toModule(module, input, moduleIds, aggregationParents))
                .toList();
        Map<String, List<String>> internalModuleIdsByCoordinate = internalModuleIdsByCoordinate(
                mavenModules, moduleIds);
        List<SourceFile> sourceFiles = input.java().compilationUnits().stream()
                .map(unit -> new SourceFile(
                        StableIdentifiers.sourceFile(unit.sourceFile().relativePath()),
                        requiredModuleId(moduleIds, unit.sourceFile().modulePomFileId()),
                        unit.sourceFile().relativePath(),
                        Language.JAVA,
                        unit.sourceFile().location()))
                .toList();
        List<Component> components = Stream.concat(
                        input.springComponents().components().stream().map(fact -> toComponent(fact, moduleIds)),
                        input.springConfiguration().configurations().stream()
                                .map(fact -> toConfiguration(fact, moduleIds)))
                .toList();
        List<Endpoint> endpoints = input.springEndpoints().endpoints().stream()
                .map(fact -> toEndpoint(fact, moduleIds))
                .toList();
        List<EntryPoint> entryPoints = input.springConfiguration().entryPoints().stream()
                .map(fact -> toEntryPoint(fact, moduleIds))
                .toList();
        List<Dependency> assembledDependencies = new ArrayList<>();
        mavenModules.forEach(
                module -> assembledDependencies.addAll(toMavenDependencies(
                        module, moduleIds, internalModuleIdsByCoordinate)));
        input.springDependencies().dependencies().stream()
                .map(fact -> toComponentDependency(fact, moduleIds))
                .forEach(assembledDependencies::add);

        List<Diagnostic> diagnostics = new ArrayList<>(Stream.of(
                        input.maven().diagnostics(),
                        input.java().diagnostics(),
                        input.springComponents().diagnostics(),
                        input.springConfiguration().diagnostics(),
                        input.springEndpoints().diagnostics(),
                        input.springDependencies().diagnostics(),
                        input.additionalDiagnostics())
                .flatMap(List::stream)
                .toList());
        modules = stabilizeModules(modules, diagnostics);
        sourceFiles = sourceFiles.stream().sorted(Comparator.comparing(SourceFile::id)).toList();
        components = stabilizeComponents(components, diagnostics);
        endpoints = stabilizeEndpoints(endpoints, diagnostics);
        entryPoints = entryPoints.stream().sorted(Comparator.comparing(EntryPoint::id)).toList();
        List<Dependency> dependencies = stabilizeDependencies(assembledDependencies, diagnostics);
        List<Diagnostic> stableDiagnostics = diagnostics.stream()
                .sorted(DIAGNOSTIC_ORDER)
                .toList();
        String projectName = value(root.metadata().artifactId()).orElse("repository");
        Evidence projectEvidence = evidence(
                "MAVEN_PROJECT", SourceLocation.file(root.pomFileId()), "maven.project.root");
        return new AnalysisReport(
                new Project(StableIdentifiers.project(root.pomFileId()), projectName,
                        BuildSystem.MAVEN, List.of(projectEvidence)),
                modules,
                sourceFiles,
                components,
                endpoints,
                entryPoints,
                List.copyOf(dependencies),
                AnalysisStatus.fromDiagnostics(stableDiagnostics),
                stableDiagnostics);
    }

    private static io.github.zhancm.repoonboard.core.model.Module toModule(
            MavenModule module,
            ProjectAnalysisInput input,
            Map<String, String> moduleIds,
            Map<String, String> aggregationParents) {
        String id = requiredModuleId(moduleIds, module.pomFileId());
        Optional<String> aggregationParentId = Optional.ofNullable(
                        aggregationParents.get(module.pomFileId()))
                .map(parentPom -> requiredModuleId(moduleIds, parentPom));
        List<String> sourceRoots = input.java().compilationUnits().stream()
                .map(unit -> unit.sourceFile())
                .filter(file -> file.modulePomFileId().equals(module.pomFileId()))
                .map(file -> file.sourceRootRelativePath())
                .distinct()
                .toList();
        List<Framework> frameworks = module.springBoot().detected()
                ? List.of(Framework.SPRING_BOOT) : List.of();
        List<Evidence> moduleEvidence = new ArrayList<>();
        moduleEvidence.add(evidence(
                "MAVEN_MODULE", SourceLocation.file(module.pomFileId()), "maven.module"));
        Optional.ofNullable(aggregationParents.get(module.pomFileId())).ifPresent(parentPom ->
                moduleEvidence.add(evidence(
                        "MAVEN_MODULE_AGGREGATION",
                        SourceLocation.file(parentPom),
                        "maven.module.aggregation")));
        return new io.github.zhancm.repoonboard.core.model.Module(
                id,
                module.pomFileId(),
                module.baseDirectory(),
                aggregationParentId,
                value(module.metadata().groupId()),
                value(module.metadata().artifactId()),
                value(module.metadata().version()),
                value(module.metadata().packaging()),
                sourceRoots,
                frameworks,
                javaVersion(module),
                springBootVersion(module),
                moduleEvidence);
    }

    private static List<LanguageVersion> javaVersion(MavenModule module) {
        for (String property : List.of(
                "maven.compiler.release", "maven.compiler.source", "java.version")) {
            Optional<MavenMetadataValue> metadata = module.metadata().property(property)
                    .filter(value -> value.resolvedValue().isPresent());
            if (metadata.isPresent()) {
                MavenMetadataValue value = metadata.orElseThrow();
                Evidence evidence = evidence(
                        "MAVEN_LANGUAGE_VERSION", value.origin(), "maven.language.version." + property);
                return List.of(new LanguageVersion(
                        Language.JAVA, value.resolvedValue().orElseThrow(), List.of(evidence)));
            }
        }
        return List.of();
    }

    private static List<FrameworkVersion> springBootVersion(MavenModule module) {
        Optional<String> version = module.springBoot().version();
        if (version.isEmpty()) {
            return List.of();
        }
        var signal = module.springBoot().evidence().stream()
                .filter(candidate -> candidate.version().equals(version))
                .sorted(Comparator.comparing(candidate -> candidate.versionOrigin().sourceFileId()))
                .findFirst()
                .orElseThrow();
        Evidence evidence = new Evidence(
                "SPRING_BOOT_VERSION",
                signal.versionOrigin(),
                List.of(signal.origin()),
                "maven.framework.spring-boot.version");
        return List.of(new FrameworkVersion(
                Framework.SPRING_BOOT, version.orElseThrow(), List.of(evidence)));
    }

    private static Component toComponent(SpringComponentFact fact, Map<String, String> moduleIds) {
        return new Component(
                componentId(fact.modulePomFileId(), fact.qualifiedName()),
                requiredModuleId(moduleIds, fact.modulePomFileId()),
                fact.qualifiedName(),
                fact.name(),
                ComponentKind.valueOf(fact.kind().name()),
                Framework.SPRING_BOOT,
                fact.location(),
                fact.evidence());
    }

    private static Component toConfiguration(
            SpringConfigurationFact fact, Map<String, String> moduleIds) {
        return new Component(
                componentId(fact.modulePomFileId(), fact.qualifiedName()),
                requiredModuleId(moduleIds, fact.modulePomFileId()),
                fact.qualifiedName(),
                Optional.empty(),
                ComponentKind.CONFIGURATION,
                Framework.SPRING_BOOT,
                fact.location(),
                fact.evidence());
    }

    private static Endpoint toEndpoint(SpringEndpointFact fact, Map<String, String> moduleIds) {
        String componentId = componentId(fact.modulePomFileId(), fact.controllerQualifiedName());
        EndpointConditions conditions = new EndpointConditions(
                fact.conditions().params(),
                fact.conditions().headers(),
                fact.conditions().consumes(),
                fact.conditions().produces(),
                fact.conditions().unresolved());
        return new Endpoint(
                StableIdentifiers.endpoint(componentId, fact.handlerMethod(), fact.httpMethod().name(),
                        fact.path(), fact.unresolvedPath(), conditions),
                requiredModuleId(moduleIds, fact.modulePomFileId()),
                componentId,
                fact.httpMethod().name(),
                fact.path(),
                fact.unresolvedPath(),
                fact.handlerMethod(),
                conditions,
                Framework.SPRING_BOOT,
                fact.location(),
                fact.evidence());
    }

    private static EntryPoint toEntryPoint(
            SpringEntryPointFact fact, Map<String, String> moduleIds) {
        return new EntryPoint(
                StableIdentifiers.entryPoint(
                        requiredModuleId(moduleIds, fact.modulePomFileId()),
                        fact.qualifiedName(), fact.kind().name()),
                requiredModuleId(moduleIds, fact.modulePomFileId()),
                fact.qualifiedName(),
                fact.kind().name(),
                Framework.SPRING_BOOT,
                fact.location(),
                fact.evidence());
    }

    private static List<Dependency> toMavenDependencies(
            MavenModule module,
            Map<String, String> moduleIds,
            Map<String, List<String>> internalModuleIdsByCoordinate) {
        List<Dependency> result = new ArrayList<>();
        for (MavenDependency dependency : module.dependencies()) {
            String coordinates = coordinate(dependency);
            SourceLocation location = dependency.artifactId().origin();
            String sourceId = requiredModuleId(moduleIds, module.pomFileId());
            List<String> internalTargets = resolvedCoordinate(dependency)
                    .map(value -> internalModuleIdsByCoordinate.getOrDefault(value, List.of()))
                    .orElse(List.of());
            Optional<String> targetId = internalTargets.size() == 1
                    ? Optional.of(internalTargets.getFirst())
                    : internalTargets.isEmpty()
                            ? Optional.of(StableIdentifiers.externalArtifact(coordinates))
                            : Optional.empty();
            ResolutionStatus status = internalTargets.size() > 1
                    ? ResolutionStatus.AMBIGUOUS : ResolutionStatus.CONFIRMED;
            result.add(new Dependency(
                    StableIdentifiers.dependency(
                            DependencyKind.MAVEN_DECLARATION, sourceId, targetId, coordinates),
                    sourceId,
                    targetId,
                    coordinates,
                    DependencyKind.MAVEN_DECLARATION,
                    status,
                    location,
                    List.of(evidence("MAVEN_DEPENDENCY", location, "maven.dependency.declaration"))));
        }
        return result;
    }

    private static Map<String, List<String>> internalModuleIdsByCoordinate(
            List<MavenModule> modules, Map<String, String> moduleIds) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (var module : modules) {
            var metadata = module.metadata();
            if (metadata.groupId().resolvedValue().isPresent()
                    && metadata.artifactId().resolvedValue().isPresent()
                    && metadata.version().resolvedValue().isPresent()) {
                String coordinate = String.join(":",
                        metadata.groupId().resolvedValue().orElseThrow(),
                        metadata.artifactId().resolvedValue().orElseThrow(),
                        metadata.version().resolvedValue().orElseThrow());
                result.computeIfAbsent(coordinate, ignored -> new ArrayList<>())
                        .add(requiredModuleId(moduleIds, module.pomFileId()));
            }
        }
        result.replaceAll((coordinate, ids) -> ids.stream().sorted().toList());
        return Map.copyOf(result);
    }

    private static Optional<String> resolvedCoordinate(MavenDependency dependency) {
        if (dependency.groupId().resolvedValue().isEmpty()
                || dependency.artifactId().resolvedValue().isEmpty()
                || dependency.version().resolvedValue().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(":",
                dependency.groupId().resolvedValue().orElseThrow(),
                dependency.artifactId().resolvedValue().orElseThrow(),
                dependency.version().resolvedValue().orElseThrow()));
    }

    private static Dependency toComponentDependency(
            SpringComponentDependencyFact fact, Map<String, String> moduleIds) {
        String sourceId = componentId(fact.sourceModulePomFileId(), fact.sourceQualifiedName());
        Optional<String> targetId = fact.targetModulePomFileId().flatMap(module ->
                fact.targetQualifiedName().map(name -> componentId(module, name)));
        ResolutionStatus status = switch (fact.status()) {
            case CONFIRMED -> ResolutionStatus.CONFIRMED;
            case AMBIGUOUS_INJECTION, AMBIGUOUS_TARGET -> ResolutionStatus.AMBIGUOUS;
            case UNRESOLVED_TARGET -> ResolutionStatus.UNRESOLVED;
        };
        return new Dependency(
                StableIdentifiers.dependency(
                        DependencyKind.COMPONENT_INJECTION, sourceId, targetId,
                        fact.declaredTargetType()),
                sourceId,
                targetId,
                fact.declaredTargetType(),
                DependencyKind.COMPONENT_INJECTION,
                status,
                fact.location(),
                fact.evidence());
    }

    private static String coordinate(MavenDependency dependency) {
        return Stream.of(dependency.groupId(), dependency.artifactId(), dependency.version())
                .map(ProjectModelAssembler::displayValue)
                .reduce((left, right) -> left + ":" + right)
                .orElseThrow();
    }

    private static String displayValue(MavenMetadataValue metadata) {
        return value(metadata).orElse("?");
    }

    private static Optional<String> value(MavenMetadataValue metadata) {
        return metadata.resolvedValue().or(() -> metadata.rawValue());
    }

    private static List<MavenModule> flatten(MavenModule root) {
        List<MavenModule> modules = new ArrayList<>();
        collect(root, modules);
        return List.copyOf(modules);
    }

    private static void collect(MavenModule module, List<MavenModule> modules) {
        modules.add(module);
        module.children().forEach(child -> collect(child, modules));
    }

    private static Map<String, String> aggregationParents(MavenModule root) {
        Map<String, String> parents = new LinkedHashMap<>();
        collectAggregationParents(root, parents);
        return Map.copyOf(parents);
    }

    private static void collectAggregationParents(
            MavenModule parent, Map<String, String> parents) {
        for (MavenModule child : parent.children()) {
            parents.put(child.pomFileId(), parent.pomFileId());
            collectAggregationParents(child, parents);
        }
    }

    private static String requiredModuleId(Map<String, String> moduleIds, String pomFileId) {
        String id = moduleIds.get(pomFileId);
        if (id == null) {
            throw new IllegalArgumentException("Analyzer fact refers to unknown module: " + pomFileId);
        }
        return id;
    }

    private static String moduleId(String pomFileId) {
        return StableIdentifiers.module(pomFileId);
    }

    private static String componentId(String pomFileId, String qualifiedName) {
        return StableIdentifiers.component(moduleId(pomFileId), qualifiedName);
    }

    private static Evidence evidence(String type, SourceLocation location, String ruleId) {
        return new Evidence(type, location, List.of(), ruleId);
    }

    private static final Comparator<Diagnostic> DIAGNOSTIC_ORDER = Comparator
            .comparing(Diagnostic::code)
            .thenComparing(diagnostic -> diagnostic.moduleId().orElse(""))
            .thenComparing(diagnostic -> diagnostic.fileId().orElse(""))
            .thenComparing(diagnostic -> diagnostic.location()
                    .flatMap(location -> location.startLine().isPresent()
                            ? Optional.of(location.startLine().getAsInt()) : Optional.empty())
                    .orElse(0))
            .thenComparing(Diagnostic::message);

    private static List<io.github.zhancm.repoonboard.core.model.Module> stabilizeModules(
            List<io.github.zhancm.repoonboard.core.model.Module> values,
            List<Diagnostic> diagnostics) {
        List<io.github.zhancm.repoonboard.core.model.Module> sorted = values.stream()
                .sorted(Comparator.comparing(io.github.zhancm.repoonboard.core.model.Module::id)
                .thenComparing(io.github.zhancm.repoonboard.core.model.Module::pomFileId)
                .thenComparing(Object::toString))
                .toList();
        return disambiguate(sorted, io.github.zhancm.repoonboard.core.model.Module::id,
                module -> module.evidence().getFirst().location(),
                (module, id) -> new io.github.zhancm.repoonboard.core.model.Module(
                        id, module.pomFileId(), module.baseDirectory(),
                        module.aggregationParentModuleId(), module.groupId(), module.artifactId(),
                        module.version(), module.packaging(), module.sourceRoots(), module.frameworks(),
                        module.languageVersions(), module.frameworkVersions(), module.evidence()),
                diagnostics, "MODULE");
    }

    private static List<Component> stabilizeComponents(
            List<Component> values, List<Diagnostic> diagnostics) {
        List<Component> sorted = values.stream().sorted(Comparator.comparing(Component::id)
                .thenComparing(component -> component.kind().name())
                .thenComparing(component -> component.location().sourceFileId())
                .thenComparing(Object::toString)).toList();
        return disambiguate(sorted, Component::id, Component::location,
                (component, id) -> new Component(id, component.moduleId(), component.qualifiedName(),
                        component.name(), component.kind(), component.framework(), component.location(),
                        component.evidence()), diagnostics, "COMPONENT");
    }

    private static List<Endpoint> stabilizeEndpoints(
            List<Endpoint> values, List<Diagnostic> diagnostics) {
        List<Endpoint> sorted = values.stream().sorted(Comparator.comparing(Endpoint::id)
                .thenComparing(endpoint -> endpoint.location().sourceFileId())
                .thenComparingInt(endpoint -> endpoint.location().startLine().orElse(0))
                .thenComparing(Object::toString)).toList();
        return disambiguate(sorted, Endpoint::id, Endpoint::location,
                (endpoint, id) -> new Endpoint(id, endpoint.moduleId(), endpoint.componentId(),
                        endpoint.httpMethod(), endpoint.path(), endpoint.unresolvedPath(),
                        endpoint.handlerMethod(), endpoint.conditions(), endpoint.framework(),
                        endpoint.location(), endpoint.evidence()), diagnostics, "ENDPOINT");
    }

    private static List<Dependency> stabilizeDependencies(
            List<Dependency> values, List<Diagnostic> diagnostics) {
        List<Dependency> sorted = values.stream().sorted(Comparator.comparing(Dependency::id)
                .thenComparing(dependency -> dependency.location().sourceFileId())
                .thenComparingInt(dependency -> dependency.location().startLine().orElse(0))
                .thenComparing(Object::toString)).toList();
        return disambiguate(sorted, Dependency::id, Dependency::location,
                (dependency, id) -> new Dependency(id, dependency.sourceId(), dependency.targetId(),
                        dependency.declaredTarget(), dependency.kind(), dependency.status(),
                        dependency.location(), dependency.evidence()), diagnostics, "DEPENDENCY");
    }

    private static <T> List<T> disambiguate(
            List<T> sorted,
            java.util.function.Function<T, String> id,
            java.util.function.Function<T, SourceLocation> location,
            java.util.function.BiFunction<T, String, T> withId,
            List<Diagnostic> diagnostics,
            String entityKind) {
        List<T> result = new ArrayList<>();
        int cursor = 0;
        while (cursor < sorted.size()) {
            int end = cursor + 1;
            String baseId = id.apply(sorted.get(cursor));
            while (end < sorted.size() && id.apply(sorted.get(end)).equals(baseId)) {
                end++;
            }
            if (end - cursor > 1) {
                SourceLocation collisionLocation = location.apply(sorted.get(cursor));
                diagnostics.add(new Diagnostic(
                        "REPORT_ID_COLLISION",
                        io.github.zhancm.repoonboard.core.model.DiagnosticSeverity.WARNING,
                        "REPORT_ASSEMBLY",
                        Optional.empty(),
                        Optional.of(collisionLocation.sourceFileId()),
                        Optional.of(collisionLocation),
                        entityKind + " key occurs " + (end - cursor) + " times: " + baseId));
            }
            for (int index = cursor; index < end; index++) {
                String resolvedId = index == cursor ? baseId : baseId + "~" + (index - cursor + 1);
                result.add(withId.apply(sorted.get(index), resolvedId));
            }
            cursor = end;
        }
        return List.copyOf(result);
    }
}
