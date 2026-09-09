package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;

/** Complete public output of a repository analysis. */
public record AnalysisReport(
        Project project,
        List<Module> modules,
        List<SourceFile> sourceFiles,
        List<Component> components,
        List<Endpoint> endpoints,
        List<EntryPoint> entryPoints,
        List<Dependency> dependencies,
        AnalysisStatus status,
        List<Diagnostic> diagnostics) {
    public AnalysisReport {
        project = Objects.requireNonNull(project, "project");
        modules = List.copyOf(Objects.requireNonNull(modules, "modules"));
        sourceFiles = List.copyOf(Objects.requireNonNull(sourceFiles, "sourceFiles"));
        components = List.copyOf(Objects.requireNonNull(components, "components"));
        endpoints = List.copyOf(Objects.requireNonNull(endpoints, "endpoints"));
        entryPoints = List.copyOf(Objects.requireNonNull(entryPoints, "entryPoints"));
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
        status = Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
