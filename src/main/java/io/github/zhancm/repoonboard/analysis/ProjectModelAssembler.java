package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.analyzer.maven.MavenDependency;
import io.github.zhancm.repoonboard.analyzer.maven.MavenMetadataValue;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationFact;
import io.github.zhancm.repoonboard.analyzer.spring.SpringDependencyStatus;
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
import io.github.zhancm.repoonboard.core.model.Language;
import io.github.zhancm.repoonboard.core.model.Project;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import io.github.zhancm.repoonboard.core.model.SourceFile;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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

        List<io.github.zhancm.repoonboard.core.model.Module> modules = mavenModules.stream()
                .map(module -> toModule(module, input, moduleIds.get(module.pomFileId())))
                .toList();
        List<SourceFile> sourceFiles = input.java().compilationUnits().stream()
                .map(unit -> new SourceFile(
                        "source:" + unit.sourceFile().relativePath(),
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
        List<Dependency> dependencies = new ArrayList<>();
        mavenModules.forEach(module -> dependencies.addAll(toMavenDependencies(module, moduleIds)));
        input.springDependencies().dependencies().stream()
                .map(fact -> toComponentDependency(fact, moduleIds))
                .forEach(dependencies::add);

        List<Diagnostic> diagnostics = Stream.of(
                        input.maven().diagnostics(),
                        input.java().diagnostics(),
                        input.springComponents().diagnostics(),
                        input.springConfiguration().diagnostics(),
                        input.springEndpoints().diagnostics(),
                        input.springDependencies().diagnostics())
                .flatMap(List::stream)
                .toList();
        String projectName = value(root.metadata().artifactId()).orElse("repository");
        Evidence projectEvidence = evidence(
                "MAVEN_PROJECT", SourceLocation.file(root.pomFileId()), "maven.project.root");
        return new AnalysisReport(
                new Project("project", projectName, BuildSystem.MAVEN, List.of(projectEvidence)),
                modules,
                sourceFiles,
                components,
                endpoints,
                entryPoints,
                List.copyOf(dependencies),
                AnalysisStatus.fromDiagnostics(diagnostics),
                diagnostics);
    }

    private static io.github.zhancm.repoonboard.core.model.Module toModule(
            MavenModule module, ProjectAnalysisInput input, String id) {
        List<String> sourceRoots = input.java().compilationUnits().stream()
                .map(unit -> unit.sourceFile())
                .filter(file -> file.modulePomFileId().equals(module.pomFileId()))
                .map(file -> file.sourceRootRelativePath())
                .distinct()
                .toList();
        List<Framework> frameworks = module.springBoot().detected()
                ? List.of(Framework.SPRING_BOOT) : List.of();
        return new io.github.zhancm.repoonboard.core.model.Module(
                id,
                module.pomFileId(),
                module.baseDirectory(),
                value(module.metadata().groupId()),
                value(module.metadata().artifactId()),
                value(module.metadata().version()),
                value(module.metadata().packaging()),
                sourceRoots,
                frameworks,
                List.of(evidence(
                        "MAVEN_MODULE", SourceLocation.file(module.pomFileId()), "maven.module")));
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
        String pathKey = fact.path().orElse("<unresolved>");
        return new Endpoint(
                "endpoint:" + componentId + ":" + fact.httpMethod() + ":" + pathKey + ":"
                        + fact.handlerMethod(),
                requiredModuleId(moduleIds, fact.modulePomFileId()),
                componentId,
                fact.httpMethod().name(),
                fact.path(),
                fact.unresolvedPath(),
                fact.handlerMethod(),
                new EndpointConditions(
                        fact.conditions().params(),
                        fact.conditions().headers(),
                        fact.conditions().consumes(),
                        fact.conditions().produces(),
                        fact.conditions().unresolved()),
                Framework.SPRING_BOOT,
                fact.location(),
                fact.evidence());
    }

    private static EntryPoint toEntryPoint(
            SpringEntryPointFact fact, Map<String, String> moduleIds) {
        return new EntryPoint(
                "entry-point:" + componentId(fact.modulePomFileId(), fact.qualifiedName()),
                requiredModuleId(moduleIds, fact.modulePomFileId()),
                fact.qualifiedName(),
                fact.kind().name(),
                Framework.SPRING_BOOT,
                fact.location(),
                fact.evidence());
    }

    private static List<Dependency> toMavenDependencies(
            MavenModule module, Map<String, String> moduleIds) {
        List<Dependency> result = new ArrayList<>();
        for (int index = 0; index < module.dependencies().size(); index++) {
            MavenDependency dependency = module.dependencies().get(index);
            String coordinates = coordinate(dependency);
            SourceLocation location = dependency.artifactId().origin();
            result.add(new Dependency(
                    "dependency:maven:" + moduleId(module.pomFileId()) + ":" + coordinates + ":" + index,
                    requiredModuleId(moduleIds, module.pomFileId()),
                    Optional.of("artifact:" + coordinates),
                    coordinates,
                    DependencyKind.MAVEN_DECLARATION,
                    ResolutionStatus.CONFIRMED,
                    location,
                    List.of(evidence("MAVEN_DEPENDENCY", location, "maven.dependency.declaration"))));
        }
        return result;
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
        String targetKey = targetId.orElse(fact.declaredTargetType());
        return new Dependency(
                "dependency:component:" + sourceId + ":" + targetKey,
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

    private static String requiredModuleId(Map<String, String> moduleIds, String pomFileId) {
        String id = moduleIds.get(pomFileId);
        if (id == null) {
            throw new IllegalArgumentException("Analyzer fact refers to unknown module: " + pomFileId);
        }
        return id;
    }

    private static String moduleId(String pomFileId) {
        return "module:" + pomFileId;
    }

    private static String componentId(String pomFileId, String qualifiedName) {
        return "component:" + moduleId(pomFileId) + ":" + qualifiedName;
    }

    private static Evidence evidence(String type, SourceLocation location, String ruleId) {
        return new Evidence(type, location, List.of(), ruleId);
    }
}
