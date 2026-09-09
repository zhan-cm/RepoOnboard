package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/** Deterministic overview counts derived from the entities in an analysis report. */
public record AnalysisSummary(
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
        AnalysisStatus analysisStatus,
        List<String> coverageLimitationCodes) {

    public AnalysisSummary {
        requireNonNegative(moduleCount, "moduleCount");
        requireNonNegative(sourceFileCount, "sourceFileCount");
        requireNonNegative(componentCount, "componentCount");
        requireNonNegative(controllerCount, "controllerCount");
        requireNonNegative(serviceCount, "serviceCount");
        requireNonNegative(repositoryCount, "repositoryCount");
        requireNonNegative(configurationCount, "configurationCount");
        requireNonNegative(endpointCount, "endpointCount");
        requireNonNegative(entryPointCount, "entryPointCount");
        requireNonNegative(dependencyCount, "dependencyCount");
        analysisStatus = Objects.requireNonNull(analysisStatus, "analysisStatus");
        coverageLimitationCodes = Objects.requireNonNull(
                        coverageLimitationCodes,
                        "coverageLimitationCodes")
                .stream()
                .map(code -> ModelValues.requireText(code, "coverageLimitationCode"))
                .distinct()
                .sorted()
                .toList();
    }

    /** Creates a summary directly from report entities so stored counts cannot drift. */
    public static AnalysisSummary from(AnalysisReport report) {
        Objects.requireNonNull(report, "report");
        List<Component> components = unique(report.components(), Component::id);
        return new AnalysisSummary(
                unique(report.modules(), Module::id).size(),
                unique(report.sourceFiles(), SourceFile::id).size(),
                components.size(),
                countKinds(components, ComponentKind.CONTROLLER, ComponentKind.REST_CONTROLLER),
                countKinds(components, ComponentKind.SERVICE),
                countKinds(components, ComponentKind.REPOSITORY),
                countKinds(components, ComponentKind.CONFIGURATION),
                unique(report.endpoints(), Endpoint::id).size(),
                unique(report.entryPoints(), EntryPoint::id).size(),
                unique(report.dependencies(), Dependency::id).size(),
                report.status(),
                report.diagnostics().stream()
                        .filter(diagnostic -> diagnostic.severity() != DiagnosticSeverity.INFO)
                        .map(Diagnostic::code)
                        .distinct()
                        .sorted()
                        .toList());
    }

    /** Indicates that one or more analysis stages may not be fully represented by the counts. */
    public boolean coverageLimited() {
        return analysisStatus != AnalysisStatus.SUCCESS || !coverageLimitationCodes.isEmpty();
    }

    private static int countKinds(List<Component> components, ComponentKind... kinds) {
        List<ComponentKind> accepted = List.of(kinds);
        return (int) components.stream().filter(component -> accepted.contains(component.kind())).count();
    }

    private static <T> List<T> unique(List<T> values, Function<T, String> id) {
        TreeMap<String, T> byId = new TreeMap<>();
        for (T value : Objects.requireNonNull(values, "values")) {
            T item = Objects.requireNonNull(value, "value");
            byId.putIfAbsent(Objects.requireNonNull(id.apply(item), "id"), item);
        }
        return List.copyOf(byId.values());
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
