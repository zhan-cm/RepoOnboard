package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Comparator;
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
        modules = stable(modules, Module::id, "modules");
        sourceFiles = stable(sourceFiles, SourceFile::id, "sourceFiles");
        components = stable(components, Component::id, "components");
        endpoints = stable(endpoints, Endpoint::id, "endpoints");
        entryPoints = stable(entryPoints, EntryPoint::id, "entryPoints");
        dependencies = stable(dependencies, Dependency::id, "dependencies");
        status = Objects.requireNonNull(status, "status");
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics").stream()
                .sorted(Comparator.comparing(Diagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId().orElse(""))
                        .thenComparing(diagnostic -> diagnostic.fileId().orElse(""))
                        .thenComparing(diagnostic -> diagnostic.location()
                                .map(SourceLocation::toString).orElse(""))
                        .thenComparing(Diagnostic::message))
                .toList();
    }

    /** Returns overview counts derived from the current stable entity identities. */
    public AnalysisSummary summary() {
        return AnalysisSummary.from(this);
    }

    private static <T> List<T> stable(
            List<T> values,
            java.util.function.Function<T, String> id,
            String name) {
        return Objects.requireNonNull(values, name).stream()
                .sorted(Comparator.comparing(id).thenComparing(Object::toString))
                .toList();
    }
}
