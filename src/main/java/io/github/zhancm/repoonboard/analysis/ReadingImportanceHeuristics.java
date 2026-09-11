package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.Component;
import io.github.zhancm.repoonboard.core.model.ComponentKind;
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Endpoint;
import io.github.zhancm.repoonboard.core.model.EntryPoint;
import io.github.zhancm.repoonboard.core.model.Module;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Deterministic importance rules derived only from confirmed report facts. */
public final class ReadingImportanceHeuristics {
    private static final Map<ReadingImportanceRule, Integer> RULE_PRIORITY = Map.of(
            ReadingImportanceRule.ROOT_POM, 0,
            ReadingImportanceRule.APPLICATION_ENTRY_POINT, 1,
            ReadingImportanceRule.CONFIGURATION, 2,
            ReadingImportanceRule.API_CONTROLLER, 3,
            ReadingImportanceRule.CONFIRMED_DEPENDENCY_NEIGHBOR, 4);

    private static final Comparator<ReadingImportanceAssessment> IMPORTANCE_ORDER =
            Comparator.comparingInt((ReadingImportanceAssessment value) -> RULE_PRIORITY.get(value.rule()))
                    .thenComparingInt(value -> value.dependencyDistance().orElse(0))
                    .thenComparing(ReadingImportanceAssessment::endpointCount, Comparator.reverseOrder())
                    .thenComparing(ReadingImportanceHeuristics::confirmedDegree, Comparator.reverseOrder())
                    .thenComparing(ReadingImportanceAssessment::confirmedIncomingDependencyCount,
                            Comparator.reverseOrder())
                    .thenComparing(value -> value.moduleId().orElse(""))
                    .thenComparing(ReadingImportanceAssessment::sourceFileId)
                    .thenComparing(ReadingImportanceAssessment::subjectId);

    /** Evaluates importance signals without deduplicating files or limiting result count. */
    public List<ReadingImportanceAssessment> evaluate(AnalysisReport report) {
        Objects.requireNonNull(report, "report");
        Map<String, Module> modules = uniqueById(report.modules(), Module::id);
        Map<String, Component> components = uniqueById(report.components(), Component::id);
        Map<String, List<String>> endpointIdsByComponent = endpointIdsByComponent(report.endpoints());
        List<Dependency> confirmedDependencies = confirmedComponentDependencies(
                report.dependencies(), components.keySet());
        Map<String, DependencyCounts> dependencyCounts = dependencyCounts(confirmedDependencies);

        List<ReadingImportanceAssessment> result = new ArrayList<>();
        modules.values().stream()
                .filter(module -> module.pomFileId().equals("pom.xml"))
                .sorted(Comparator.comparing(Module::id))
                .findFirst()
                .ifPresent(module -> result.add(assessment(
                        module.pomFileId(), Optional.of(module.id()), module.id(),
                        ReadingImportanceRule.ROOT_POM, 0, counts(module.id(), dependencyCounts),
                        Optional.empty(), List.of())));

        for (EntryPoint entryPoint : uniqueById(report.entryPoints(), EntryPoint::id).values()) {
            result.add(assessment(
                    entryPoint.location().sourceFileId(), Optional.of(entryPoint.moduleId()), entryPoint.id(),
                    ReadingImportanceRule.APPLICATION_ENTRY_POINT, 0,
                    counts(entryPoint.id(), dependencyCounts), Optional.empty(), List.of()));
        }

        Set<String> seedComponentIds = new TreeSet<>();
        Map<String, ReadingImportanceAssessment> componentSignals = new LinkedHashMap<>();
        for (Component component : components.values()) {
            if (component.kind() == ComponentKind.CONFIGURATION) {
                seedComponentIds.add(component.id());
                componentSignals.put("configuration:" + component.id(), assessment(
                        component.location().sourceFileId(), Optional.of(component.moduleId()), component.id(),
                        ReadingImportanceRule.CONFIGURATION, 0,
                        counts(component.id(), dependencyCounts), Optional.empty(), List.of()));
            }
            int endpointCount = endpointIdsByComponent.getOrDefault(component.id(), List.of()).size();
            if (isController(component.kind()) && endpointCount > 0) {
                seedComponentIds.add(component.id());
                componentSignals.put("controller:" + component.id(), assessment(
                        component.location().sourceFileId(), Optional.of(component.moduleId()), component.id(),
                        ReadingImportanceRule.API_CONTROLLER, endpointCount,
                        counts(component.id(), dependencyCounts), Optional.empty(), List.of()));
            }
        }
        addEntryPointComponentSeeds(report.entryPoints(), components, seedComponentIds);
        result.addAll(componentSignals.values());
        result.addAll(dependencyNeighbors(
                components, confirmedDependencies, dependencyCounts, seedComponentIds));
        return result.stream().sorted(IMPORTANCE_ORDER).toList();
    }

    private static List<ReadingImportanceAssessment> dependencyNeighbors(
            Map<String, Component> components,
            List<Dependency> dependencies,
            Map<String, DependencyCounts> dependencyCounts,
            Set<String> seeds) {
        Map<String, List<Dependency>> outgoing = new TreeMap<>();
        for (Dependency dependency : dependencies) {
            outgoing.computeIfAbsent(dependency.sourceId(), ignored -> new ArrayList<>()).add(dependency);
        }
        outgoing.values().forEach(values -> values.sort(Comparator.comparing(Dependency::id)));

        Map<String, Integer> distances = new HashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (String seed : seeds) {
            distances.put(seed, 0);
            queue.add(seed);
        }
        while (!queue.isEmpty()) {
            String sourceId = queue.removeFirst();
            int nextDistance = distances.get(sourceId) + 1;
            for (Dependency dependency : outgoing.getOrDefault(sourceId, List.of())) {
                String targetId = dependency.targetId().orElseThrow();
                Component target = components.get(targetId);
                if (!isDependencyNeighborKind(target.kind()) || distances.containsKey(targetId)) {
                    continue;
                }
                distances.put(targetId, nextDistance);
                queue.addLast(targetId);
            }
        }

        List<ReadingImportanceAssessment> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : new TreeMap<>(distances).entrySet()) {
            String componentId = entry.getKey();
            int distance = entry.getValue();
            if (distance == 0) {
                continue;
            }
            Component component = components.get(componentId);
            List<String> supportingDependencies = dependencies.stream()
                    .filter(dependency -> dependency.targetId().orElseThrow().equals(componentId))
                    .filter(dependency -> distances.getOrDefault(dependency.sourceId(), -1) == distance - 1)
                    .map(Dependency::id)
                    .distinct()
                    .sorted()
                    .toList();
            result.add(assessment(
                    component.location().sourceFileId(), Optional.of(component.moduleId()), component.id(),
                    ReadingImportanceRule.CONFIRMED_DEPENDENCY_NEIGHBOR, 0,
                    counts(component.id(), dependencyCounts), Optional.of(distance), supportingDependencies));
        }
        return result;
    }

    private static void addEntryPointComponentSeeds(
            List<EntryPoint> entryPoints,
            Map<String, Component> components,
            Set<String> seeds) {
        // This is an exact public-model identity join, not a class-name heuristic.
        Set<String> entryPointKeys = new TreeSet<>();
        for (EntryPoint entryPoint : entryPoints) {
            entryPointKeys.add(entryPoint.moduleId() + "\u0000" + entryPoint.qualifiedName());
        }
        components.values().stream()
                .filter(component -> entryPointKeys.contains(
                        component.moduleId() + "\u0000" + component.qualifiedName()))
                .map(Component::id)
                .forEach(seeds::add);
    }

    private static List<Dependency> confirmedComponentDependencies(
            List<Dependency> dependencies,
            Set<String> componentIds) {
        TreeMap<String, Dependency> byId = new TreeMap<>();
        for (Dependency dependency : dependencies) {
            if (dependency.kind() == DependencyKind.COMPONENT_INJECTION
                    && dependency.status() == ResolutionStatus.CONFIRMED
                    && dependency.targetId().isPresent()
                    && componentIds.contains(dependency.sourceId())
                    && componentIds.contains(dependency.targetId().orElseThrow())) {
                byId.putIfAbsent(dependency.id(), dependency);
            }
        }
        return List.copyOf(byId.values());
    }

    private static Map<String, DependencyCounts> dependencyCounts(List<Dependency> dependencies) {
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Integer> outgoing = new HashMap<>();
        for (Dependency dependency : dependencies) {
            outgoing.merge(dependency.sourceId(), 1, Integer::sum);
            incoming.merge(dependency.targetId().orElseThrow(), 1, Integer::sum);
        }
        Map<String, DependencyCounts> result = new HashMap<>();
        Set<String> ids = new TreeSet<>();
        ids.addAll(incoming.keySet());
        ids.addAll(outgoing.keySet());
        for (String id : ids) {
            result.put(id, new DependencyCounts(
                    incoming.getOrDefault(id, 0), outgoing.getOrDefault(id, 0)));
        }
        return Map.copyOf(result);
    }

    private static Map<String, List<String>> endpointIdsByComponent(List<Endpoint> endpoints) {
        Map<String, Set<String>> ids = new TreeMap<>();
        for (Endpoint endpoint : endpoints) {
            ids.computeIfAbsent(endpoint.componentId(), ignored -> new TreeSet<>()).add(endpoint.id());
        }
        Map<String, List<String>> result = new TreeMap<>();
        ids.forEach((componentId, endpointIds) -> result.put(componentId, List.copyOf(endpointIds)));
        return Map.copyOf(result);
    }

    private static ReadingImportanceAssessment assessment(
            String sourceFileId,
            Optional<String> moduleId,
            String subjectId,
            ReadingImportanceRule rule,
            int endpointCount,
            DependencyCounts counts,
            Optional<Integer> distance,
            List<String> supportingDependencyIds) {
        return new ReadingImportanceAssessment(
                sourceFileId, moduleId, subjectId, rule, endpointCount,
                counts.incoming(), counts.outgoing(), distance, supportingDependencyIds);
    }

    private static DependencyCounts counts(
            String id,
            Map<String, DependencyCounts> dependencyCounts) {
        return dependencyCounts.getOrDefault(id, new DependencyCounts(0, 0));
    }

    private static boolean isController(ComponentKind kind) {
        return kind == ComponentKind.CONTROLLER || kind == ComponentKind.REST_CONTROLLER;
    }

    private static boolean isDependencyNeighborKind(ComponentKind kind) {
        return kind == ComponentKind.SERVICE || kind == ComponentKind.REPOSITORY;
    }

    private static int confirmedDegree(ReadingImportanceAssessment value) {
        return value.confirmedIncomingDependencyCount() + value.confirmedOutgoingDependencyCount();
    }

    private static <T> Map<String, T> uniqueById(
            List<T> values,
            java.util.function.Function<T, String> id) {
        TreeMap<String, T> result = new TreeMap<>();
        for (T value : Objects.requireNonNull(values, "values")) {
            T item = Objects.requireNonNull(value, "value");
            result.putIfAbsent(Objects.requireNonNull(id.apply(item), "id"), item);
        }
        return Map.copyOf(result);
    }

    private record DependencyCounts(int incoming, int outgoing) {}
}
