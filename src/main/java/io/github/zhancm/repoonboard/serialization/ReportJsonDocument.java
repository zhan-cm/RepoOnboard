package io.github.zhancm.repoonboard.serialization;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.AnalysisSummary;
import io.github.zhancm.repoonboard.core.model.BuildSystem;
import io.github.zhancm.repoonboard.core.model.Component;
import io.github.zhancm.repoonboard.core.model.ComponentKind;
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.Endpoint;
import io.github.zhancm.repoonboard.core.model.EndpointConditions;
import io.github.zhancm.repoonboard.core.model.EntryPoint;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.Framework;
import io.github.zhancm.repoonboard.core.model.Language;
import io.github.zhancm.repoonboard.core.model.Module;
import io.github.zhancm.repoonboard.core.model.Project;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import io.github.zhancm.repoonboard.core.model.SourceFile;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Explicit transport DTO for the versioned public analysis report. */
public record ReportJsonDocument(
        String schemaVersion,
        ProjectDto project,
        SummaryDto summary,
        List<ModuleDto> modules,
        List<SourceFileDto> sourceFiles,
        List<ComponentDto> components,
        List<EndpointDto> endpoints,
        List<EntryPointDto> entryPoints,
        List<DependencyDto> dependencies,
        String status,
        List<DiagnosticDto> diagnostics) {

    /** Maps the analyzer-neutral model to the current JSON schema. */
    public static ReportJsonDocument fromModel(AnalysisReport report) {
        Objects.requireNonNull(report, "report");
        return new ReportJsonDocument(
                ReportSchemaVersion.CURRENT.toString(),
                ProjectDto.fromModel(report.project()),
                SummaryDto.fromModel(report.summary()),
                report.modules().stream().map(ModuleDto::fromModel).toList(),
                report.sourceFiles().stream().map(SourceFileDto::fromModel).toList(),
                report.components().stream().map(ComponentDto::fromModel).toList(),
                report.endpoints().stream().map(EndpointDto::fromModel).toList(),
                report.entryPoints().stream().map(EntryPointDto::fromModel).toList(),
                report.dependencies().stream().map(DependencyDto::fromModel).toList(),
                report.status().name(),
                report.diagnostics().stream().map(DiagnosticDto::fromModel).toList());
    }

    /** Reconstructs the validated analyzer-neutral model. */
    public AnalysisReport toModel() {
        ReportSchemaVersion version = ReportSchemaVersion.parse(schemaVersion);
        if (!version.isCompatibleWith(ReportSchemaVersion.CURRENT)) {
            throw new UnsupportedReportSchemaVersionException(
                    version,
                    ReportSchemaVersion.CURRENT);
        }
        AnalysisReport report = new AnalysisReport(
                Objects.requireNonNull(project, "project").toModel(),
                requiredList(modules, "modules").stream().map(ModuleDto::toModel).toList(),
                requiredList(sourceFiles, "sourceFiles").stream()
                        .map(SourceFileDto::toModel).toList(),
                requiredList(components, "components").stream()
                        .map(ComponentDto::toModel).toList(),
                requiredList(endpoints, "endpoints").stream().map(EndpointDto::toModel).toList(),
                requiredList(entryPoints, "entryPoints").stream()
                        .map(EntryPointDto::toModel).toList(),
                requiredList(dependencies, "dependencies").stream()
                        .map(DependencyDto::toModel).toList(),
                enumValue(AnalysisStatus.class, status, "status"),
                requiredList(diagnostics, "diagnostics").stream()
                        .map(DiagnosticDto::toModel).toList());
        if (version.minor() >= 1) {
            AnalysisSummary suppliedSummary = Objects.requireNonNull(summary, "summary").toModel();
            if (!suppliedSummary.equals(report.summary())) {
                throw new IllegalArgumentException(
                        "summary must match counts derived from report entities");
            }
        }
        return report;
    }

    public record ProjectDto(
            String id,
            String name,
            String buildSystem,
            List<EvidenceDto> evidence) {
        static ProjectDto fromModel(Project project) {
            return new ProjectDto(
                    project.id(),
                    project.name(),
                    project.buildSystem().name(),
                    project.evidence().stream().map(EvidenceDto::fromModel).toList());
        }

        Project toModel() {
            return new Project(
                    id,
                    name,
                    enumValue(BuildSystem.class, buildSystem, "project.buildSystem"),
                    requiredList(evidence, "project.evidence").stream()
                            .map(EvidenceDto::toModel).toList());
        }
    }

    public record SummaryDto(
            int moduleCount,
            int sourceFileCount,
            int componentCount,
            int controllerCount,
            int serviceCount,
            int repositoryCount,
            int configurationCount,
            int endpointCount,
            int entryPointCount,
            int dependencyCount,
            String analysisStatus,
            boolean coverageLimited,
            List<String> coverageLimitationCodes) {
        static SummaryDto fromModel(AnalysisSummary summary) {
            return new SummaryDto(
                    summary.moduleCount(),
                    summary.sourceFileCount(),
                    summary.componentCount(),
                    summary.controllerCount(),
                    summary.serviceCount(),
                    summary.repositoryCount(),
                    summary.configurationCount(),
                    summary.endpointCount(),
                    summary.entryPointCount(),
                    summary.dependencyCount(),
                    summary.analysisStatus().name(),
                    summary.coverageLimited(),
                    summary.coverageLimitationCodes());
        }

        AnalysisSummary toModel() {
            AnalysisSummary result = new AnalysisSummary(
                    moduleCount,
                    sourceFileCount,
                    componentCount,
                    controllerCount,
                    serviceCount,
                    repositoryCount,
                    configurationCount,
                    endpointCount,
                    entryPointCount,
                    dependencyCount,
                    enumValue(AnalysisStatus.class, analysisStatus, "summary.analysisStatus"),
                    requiredList(
                            coverageLimitationCodes,
                            "summary.coverageLimitationCodes"));
            if (coverageLimited != result.coverageLimited()) {
                throw new IllegalArgumentException(
                        "summary.coverageLimited must match analysisStatus and limitations");
            }
            return result;
        }
    }

    public record ModuleDto(
            String id,
            String pomFileId,
            String baseDirectory,
            String groupId,
            String artifactId,
            String version,
            String packaging,
            List<String> sourceRoots,
            List<String> frameworks,
            List<EvidenceDto> evidence) {
        static ModuleDto fromModel(Module module) {
            return new ModuleDto(
                    module.id(),
                    module.pomFileId(),
                    module.baseDirectory(),
                    module.groupId().orElse(null),
                    module.artifactId().orElse(null),
                    module.version().orElse(null),
                    module.packaging().orElse(null),
                    module.sourceRoots(),
                    module.frameworks().stream().map(Enum::name).toList(),
                    module.evidence().stream().map(EvidenceDto::fromModel).toList());
        }

        Module toModel() {
            return new Module(
                    id,
                    pomFileId,
                    baseDirectory,
                    Optional.ofNullable(groupId),
                    Optional.ofNullable(artifactId),
                    Optional.ofNullable(version),
                    Optional.ofNullable(packaging),
                    requiredList(sourceRoots, "module.sourceRoots"),
                    requiredList(frameworks, "module.frameworks").stream()
                            .map(value -> enumValue(Framework.class, value, "module.frameworks"))
                            .toList(),
                    requiredList(evidence, "module.evidence").stream()
                            .map(EvidenceDto::toModel).toList());
        }
    }

    public record SourceFileDto(
            String id,
            String moduleId,
            String path,
            String language,
            SourceLocationDto location) {
        static SourceFileDto fromModel(SourceFile sourceFile) {
            return new SourceFileDto(
                    sourceFile.id(),
                    sourceFile.moduleId(),
                    sourceFile.path(),
                    sourceFile.language().name(),
                    SourceLocationDto.fromModel(sourceFile.location()));
        }

        SourceFile toModel() {
            return new SourceFile(
                    id,
                    moduleId,
                    path,
                    enumValue(Language.class, language, "sourceFile.language"),
                    Objects.requireNonNull(location, "sourceFile.location").toModel());
        }
    }

    public record ComponentDto(
            String id,
            String moduleId,
            String qualifiedName,
            String name,
            String kind,
            String framework,
            SourceLocationDto location,
            List<EvidenceDto> evidence) {
        static ComponentDto fromModel(Component component) {
            return new ComponentDto(
                    component.id(),
                    component.moduleId(),
                    component.qualifiedName(),
                    component.name().orElse(null),
                    component.kind().name(),
                    component.framework().name(),
                    SourceLocationDto.fromModel(component.location()),
                    component.evidence().stream().map(EvidenceDto::fromModel).toList());
        }

        Component toModel() {
            return new Component(
                    id,
                    moduleId,
                    qualifiedName,
                    Optional.ofNullable(name),
                    enumValue(ComponentKind.class, kind, "component.kind"),
                    enumValue(Framework.class, framework, "component.framework"),
                    Objects.requireNonNull(location, "component.location").toModel(),
                    requiredList(evidence, "component.evidence").stream()
                            .map(EvidenceDto::toModel).toList());
        }
    }

    public record EndpointDto(
            String id,
            String moduleId,
            String componentId,
            String httpMethod,
            String path,
            boolean unresolvedPath,
            String handlerMethod,
            EndpointConditionsDto conditions,
            String framework,
            SourceLocationDto location,
            List<EvidenceDto> evidence) {
        static EndpointDto fromModel(Endpoint endpoint) {
            return new EndpointDto(
                    endpoint.id(),
                    endpoint.moduleId(),
                    endpoint.componentId(),
                    endpoint.httpMethod(),
                    endpoint.path().orElse(null),
                    endpoint.unresolvedPath(),
                    endpoint.handlerMethod(),
                    EndpointConditionsDto.fromModel(endpoint.conditions()),
                    endpoint.framework().name(),
                    SourceLocationDto.fromModel(endpoint.location()),
                    endpoint.evidence().stream().map(EvidenceDto::fromModel).toList());
        }

        Endpoint toModel() {
            return new Endpoint(
                    id,
                    moduleId,
                    componentId,
                    httpMethod,
                    Optional.ofNullable(path),
                    unresolvedPath,
                    handlerMethod,
                    Objects.requireNonNull(conditions, "endpoint.conditions").toModel(),
                    enumValue(Framework.class, framework, "endpoint.framework"),
                    Objects.requireNonNull(location, "endpoint.location").toModel(),
                    requiredList(evidence, "endpoint.evidence").stream()
                            .map(EvidenceDto::toModel).toList());
        }
    }

    public record EndpointConditionsDto(
            List<String> params,
            List<String> headers,
            List<String> consumes,
            List<String> produces,
            boolean unresolved) {
        static EndpointConditionsDto fromModel(EndpointConditions conditions) {
            return new EndpointConditionsDto(
                    conditions.params(),
                    conditions.headers(),
                    conditions.consumes(),
                    conditions.produces(),
                    conditions.unresolved());
        }

        EndpointConditions toModel() {
            return new EndpointConditions(
                    requiredList(params, "endpoint.conditions.params"),
                    requiredList(headers, "endpoint.conditions.headers"),
                    requiredList(consumes, "endpoint.conditions.consumes"),
                    requiredList(produces, "endpoint.conditions.produces"),
                    unresolved);
        }
    }

    public record EntryPointDto(
            String id,
            String moduleId,
            String qualifiedName,
            String kind,
            String framework,
            SourceLocationDto location,
            List<EvidenceDto> evidence) {
        static EntryPointDto fromModel(EntryPoint entryPoint) {
            return new EntryPointDto(
                    entryPoint.id(),
                    entryPoint.moduleId(),
                    entryPoint.qualifiedName(),
                    entryPoint.kind(),
                    entryPoint.framework().name(),
                    SourceLocationDto.fromModel(entryPoint.location()),
                    entryPoint.evidence().stream().map(EvidenceDto::fromModel).toList());
        }

        EntryPoint toModel() {
            return new EntryPoint(
                    id,
                    moduleId,
                    qualifiedName,
                    kind,
                    enumValue(Framework.class, framework, "entryPoint.framework"),
                    Objects.requireNonNull(location, "entryPoint.location").toModel(),
                    requiredList(evidence, "entryPoint.evidence").stream()
                            .map(EvidenceDto::toModel).toList());
        }
    }

    public record DependencyDto(
            String id,
            String sourceId,
            String targetId,
            String declaredTarget,
            String kind,
            String status,
            SourceLocationDto location,
            List<EvidenceDto> evidence) {
        static DependencyDto fromModel(Dependency dependency) {
            return new DependencyDto(
                    dependency.id(),
                    dependency.sourceId(),
                    dependency.targetId().orElse(null),
                    dependency.declaredTarget(),
                    dependency.kind().name(),
                    dependency.status().name(),
                    SourceLocationDto.fromModel(dependency.location()),
                    dependency.evidence().stream().map(EvidenceDto::fromModel).toList());
        }

        Dependency toModel() {
            return new Dependency(
                    id,
                    sourceId,
                    Optional.ofNullable(targetId),
                    declaredTarget,
                    enumValue(DependencyKind.class, kind, "dependency.kind"),
                    enumValue(ResolutionStatus.class, status, "dependency.status"),
                    Objects.requireNonNull(location, "dependency.location").toModel(),
                    requiredList(evidence, "dependency.evidence").stream()
                            .map(EvidenceDto::toModel).toList());
        }
    }

    public record EvidenceDto(
            String type,
            SourceLocationDto location,
            List<SourceLocationDto> relatedLocations,
            String ruleId) {
        static EvidenceDto fromModel(Evidence evidence) {
            return new EvidenceDto(
                    evidence.type(),
                    SourceLocationDto.fromModel(evidence.location()),
                    evidence.relatedLocations().stream().map(SourceLocationDto::fromModel).toList(),
                    evidence.ruleId());
        }

        Evidence toModel() {
            return new Evidence(
                    type,
                    Objects.requireNonNull(location, "evidence.location").toModel(),
                    requiredList(relatedLocations, "evidence.relatedLocations").stream()
                            .map(SourceLocationDto::toModel).toList(),
                    ruleId);
        }
    }

    public record DiagnosticDto(
            String code,
            String severity,
            String stage,
            String moduleId,
            String fileId,
            SourceLocationDto location,
            String message) {
        static DiagnosticDto fromModel(Diagnostic diagnostic) {
            return new DiagnosticDto(
                    diagnostic.code(),
                    diagnostic.severity().name(),
                    diagnostic.stage(),
                    diagnostic.moduleId().orElse(null),
                    diagnostic.fileId().orElse(null),
                    diagnostic.location().map(SourceLocationDto::fromModel).orElse(null),
                    diagnostic.message());
        }

        Diagnostic toModel() {
            return new Diagnostic(
                    code,
                    enumValue(DiagnosticSeverity.class, severity, "diagnostic.severity"),
                    stage,
                    Optional.ofNullable(moduleId),
                    Optional.ofNullable(fileId),
                    Optional.ofNullable(location).map(SourceLocationDto::toModel),
                    message);
        }
    }

    public record SourceLocationDto(
            String sourceFileId,
            Integer startLine,
            Integer startColumn,
            Integer endLine,
            Integer endColumn,
            String symbol) {
        static SourceLocationDto fromModel(SourceLocation location) {
            return new SourceLocationDto(
                    location.sourceFileId(),
                    boxed(location.startLine()),
                    boxed(location.startColumn()),
                    boxed(location.endLine()),
                    boxed(location.endColumn()),
                    location.symbol().orElse(null));
        }

        SourceLocation toModel() {
            return new SourceLocation(
                    sourceFileId,
                    optionalInt(startLine),
                    optionalInt(startColumn),
                    optionalInt(endLine),
                    optionalInt(endColumn),
                    Optional.ofNullable(symbol));
        }
    }

    private static Integer boxed(OptionalInt value) {
        return value.isPresent() ? value.getAsInt() : null;
    }

    private static OptionalInt optionalInt(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    private static <T> List<T> requiredList(List<T> values, String name) {
        return Objects.requireNonNull(values, name);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> enumType,
            String value,
            String fieldName) {
        Objects.requireNonNull(value, fieldName);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown " + fieldName + " value: " + value,
                    exception);
        }
    }
}
