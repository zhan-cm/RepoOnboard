package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Component;
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Endpoint;
import io.github.zhancm.repoonboard.core.model.EntryPoint;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.Module;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/** Generates deterministic explanation text from the exact facts used by the reading path. */
public final class RecommendationExplanationGenerator {
    private static final String PARTIAL_NOTICE =
            "Recommendations are based on partial analysis and may omit important files.";
    private static final String FAILED_NOTICE =
            "Analysis failed; recommendations only reflect the facts that remain available.";
    private static final String DIAGNOSTIC_NOTICE =
            "Analysis diagnostics may limit the completeness of these recommendations.";

    private final RecommendedReadingPathGenerator pathGenerator;

    public RecommendationExplanationGenerator() {
        this(new RecommendedReadingPathGenerator());
    }

    RecommendationExplanationGenerator(RecommendedReadingPathGenerator pathGenerator) {
        this.pathGenerator = Objects.requireNonNull(pathGenerator, "pathGenerator");
    }

    public ExplainedReadingPath generate(AnalysisReport report) {
        Objects.requireNonNull(report, "report");
        RecommendedReadingPath path = pathGenerator.generate(report);
        Facts facts = Facts.from(report);
        List<RecommendationExplanation> explanations = path.items().stream()
                .map(item -> explain(item, facts))
                .toList();
        var summary = report.summary();
        return new ExplainedReadingPath(
                path,
                explanations,
                report.status(),
                summary.coverageLimitationCodes(),
                coverageNotice(report.status(), summary.coverageLimited()));
    }

    private static RecommendationExplanation explain(
            RecommendedReadingItem item,
            Facts facts) {
        List<RecommendationReason> reasons = new ArrayList<>();
        for (ReadingImportanceAssessment assessment : item.assessments()) {
            switch (assessment.rule()) {
                case ROOT_POM -> reasons.add(rootPomReason(assessment, facts));
                case APPLICATION_ENTRY_POINT -> reasons.add(entryPointReason(assessment, facts));
                case CONFIGURATION -> reasons.add(configurationReason(assessment, facts));
                case API_CONTROLLER -> reasons.add(apiControllerReason(assessment, facts));
                case CONFIRMED_DEPENDENCY_NEIGHBOR -> {
                    reasons.add(dependencyPathReason(assessment, facts));
                    addDependencyCountReasons(assessment, facts, reasons);
                }
            }
        }
        return new RecommendationExplanation(item.sourceFileId(), reasons);
    }

    private static RecommendationReason rootPomReason(
            ReadingImportanceAssessment assessment,
            Facts facts) {
        Module module = required(facts.modules(), assessment.subjectId(), "module");
        return reason(
                RecommendationReasonKind.ROOT_BUILD_FILE,
                "Root Maven build file",
                1,
                Optional.empty(),
                List.of(module.id()),
                module.evidence());
    }

    private static RecommendationReason entryPointReason(
            ReadingImportanceAssessment assessment,
            Facts facts) {
        EntryPoint entryPoint = required(facts.entryPoints(), assessment.subjectId(), "entry point");
        return reason(
                RecommendationReasonKind.APPLICATION_ENTRY_POINT,
                "Application entry point",
                1,
                Optional.empty(),
                List.of(entryPoint.id()),
                entryPoint.evidence());
    }

    private static RecommendationReason configurationReason(
            ReadingImportanceAssessment assessment,
            Facts facts) {
        Component component = required(facts.components(), assessment.subjectId(), "component");
        return reason(
                RecommendationReasonKind.CONFIGURATION_COMPONENT,
                "Configuration component",
                1,
                Optional.empty(),
                List.of(component.id()),
                component.evidence());
    }

    private static RecommendationReason apiControllerReason(
            ReadingImportanceAssessment assessment,
            Facts facts) {
        Component component = required(facts.components(), assessment.subjectId(), "component");
        List<Endpoint> endpoints = facts.endpointsByComponent()
                .getOrDefault(component.id(), List.of());
        List<String> entityIds = new ArrayList<>();
        entityIds.add(component.id());
        entityIds.addAll(endpoints.stream().map(Endpoint::id).toList());
        List<Evidence> evidence = new ArrayList<>(component.evidence());
        endpoints.forEach(endpoint -> evidence.addAll(endpoint.evidence()));
        return reason(
                RecommendationReasonKind.HTTP_ENDPOINT_EXPOSURE,
                "Exposes " + endpoints.size() + " HTTP " + plural(endpoints.size(), "endpoint"),
                endpoints.size(),
                Optional.empty(),
                entityIds,
                evidence);
    }

    private static RecommendationReason dependencyPathReason(
            ReadingImportanceAssessment assessment,
            Facts facts) {
        Component component = required(facts.components(), assessment.subjectId(), "component");
        List<Dependency> dependencies = assessment.supportingDependencyIds().stream()
                .map(id -> requiredConfirmedDependency(facts.dependencies(), id))
                .toList();
        int distance = assessment.dependencyDistance().orElseThrow();
        List<String> entityIds = new ArrayList<>();
        entityIds.add(component.id());
        entityIds.addAll(dependencies.stream().map(Dependency::id).toList());
        List<Evidence> evidence = new ArrayList<>(component.evidence());
        dependencies.forEach(dependency -> evidence.addAll(dependency.evidence()));
        return reason(
                RecommendationReasonKind.CONFIRMED_DEPENDENCY_PATH,
                "Reached from a higher-priority component at confirmed dependency distance " + distance,
                dependencies.size(),
                Optional.of(distance),
                entityIds,
                evidence);
    }

    private static void addDependencyCountReasons(
            ReadingImportanceAssessment assessment,
            Facts facts,
            List<RecommendationReason> reasons) {
        List<Dependency> incoming = facts.confirmedDependencies().stream()
                .filter(dependency -> dependency.targetId().orElseThrow().equals(assessment.subjectId()))
                .toList();
        List<Dependency> outgoing = facts.confirmedDependencies().stream()
                .filter(dependency -> dependency.sourceId().equals(assessment.subjectId()))
                .toList();
        if (!incoming.isEmpty()) {
            reasons.add(dependencyCountReason(
                    RecommendationReasonKind.CONFIRMED_INCOMING_DEPENDENCIES,
                    "Used by " + incoming.size() + " confirmed "
                            + plural(incoming.size(), "component"),
                    incoming));
        }
        if (!outgoing.isEmpty()) {
            reasons.add(dependencyCountReason(
                    RecommendationReasonKind.CONFIRMED_OUTGOING_DEPENDENCIES,
                    "Depends on " + outgoing.size() + " confirmed "
                            + plural(outgoing.size(), "component"),
                    outgoing));
        }
    }

    private static RecommendationReason dependencyCountReason(
            RecommendationReasonKind kind,
            String message,
            List<Dependency> dependencies) {
        List<Evidence> evidence = dependencies.stream()
                .flatMap(dependency -> dependency.evidence().stream())
                .toList();
        return reason(
                kind,
                message,
                dependencies.size(),
                Optional.empty(),
                dependencies.stream().map(Dependency::id).toList(),
                evidence);
    }

    private static RecommendationReason reason(
            RecommendationReasonKind kind,
            String message,
            int factCount,
            Optional<Integer> distance,
            List<String> entityIds,
            List<Evidence> evidence) {
        return new RecommendationReason(
                kind, message, factCount, distance, entityIds, evidence);
    }

    private static Dependency requiredConfirmedDependency(
            Map<String, Dependency> dependencies,
            String id) {
        Dependency dependency = required(dependencies, id, "dependency");
        if (dependency.kind() != DependencyKind.COMPONENT_INJECTION
                || dependency.status() != ResolutionStatus.CONFIRMED
                || dependency.targetId().isEmpty()) {
            throw new IllegalArgumentException("Recommendation dependency is not confirmed: " + id);
        }
        return dependency;
    }

    private static Optional<String> coverageNotice(
            AnalysisStatus status,
            boolean coverageLimited) {
        if (status == AnalysisStatus.PARTIAL) {
            return Optional.of(PARTIAL_NOTICE);
        }
        if (status == AnalysisStatus.FAILED) {
            return Optional.of(FAILED_NOTICE);
        }
        return coverageLimited ? Optional.of(DIAGNOSTIC_NOTICE) : Optional.empty();
    }

    private static String plural(int count, String word) {
        return count == 1 ? word : word + "s";
    }

    private static <T> T required(Map<String, T> values, String id, String kind) {
        T value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + kind + " identity: " + id);
        }
        return value;
    }

    private static <T> Map<String, T> uniqueById(
            List<T> values,
            Function<T, String> id) {
        TreeMap<String, T> result = new TreeMap<>();
        for (T value : values) {
            result.putIfAbsent(id.apply(value), value);
        }
        return Map.copyOf(result);
    }

    private record Facts(
            Map<String, Module> modules,
            Map<String, Component> components,
            Map<String, EntryPoint> entryPoints,
            Map<String, Dependency> dependencies,
            Map<String, List<Endpoint>> endpointsByComponent,
            List<Dependency> confirmedDependencies) {

        private static Facts from(AnalysisReport report) {
            Map<String, Module> modules = uniqueById(report.modules(), Module::id);
            Map<String, Component> components = uniqueById(report.components(), Component::id);
            Map<String, Endpoint> endpoints = uniqueById(report.endpoints(), Endpoint::id);
            Map<String, EntryPoint> entryPoints = uniqueById(report.entryPoints(), EntryPoint::id);
            Map<String, Dependency> dependencies = uniqueById(report.dependencies(), Dependency::id);
            Map<String, List<Endpoint>> endpointsByComponent = new TreeMap<>();
            endpoints.values().stream()
                    .sorted(java.util.Comparator.comparing(Endpoint::id))
                    .forEach(endpoint -> endpointsByComponent
                            .computeIfAbsent(endpoint.componentId(), ignored -> new ArrayList<>())
                            .add(endpoint));
            List<Dependency> confirmedDependencies = dependencies.values().stream()
                    .filter(dependency -> dependency.kind() == DependencyKind.COMPONENT_INJECTION)
                    .filter(dependency -> dependency.status() == ResolutionStatus.CONFIRMED)
                    .filter(dependency -> dependency.targetId().isPresent())
                    .filter(dependency -> components.containsKey(dependency.sourceId()))
                    .filter(dependency -> components.containsKey(dependency.targetId().orElseThrow()))
                    .sorted(java.util.Comparator.comparing(Dependency::id))
                    .toList();
            return new Facts(
                    modules,
                    components,
                    entryPoints,
                    dependencies,
                    Map.copyOf(endpointsByComponent),
                    confirmedDependencies);
        }
    }
}
