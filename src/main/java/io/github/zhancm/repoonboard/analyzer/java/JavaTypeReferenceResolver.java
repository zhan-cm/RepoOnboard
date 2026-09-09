package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.analyzer.maven.MavenDependency;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalysis;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Resolves only confirmed, visible project-local type declarations. */
public final class JavaTypeReferenceResolver {

    public JavaParseAnalysis resolve(JavaParseAnalysis parsed, MavenModuleAnalysis modules) {
        return resolve(parsed, modules, JavaDeclarationIndex.build(parsed));
    }

    public JavaParseAnalysis resolve(
            JavaParseAnalysis parsed,
            MavenModuleAnalysis modules,
            JavaDeclarationIndex index) {
        Objects.requireNonNull(parsed, "parsed");
        Objects.requireNonNull(modules, "modules");
        Objects.requireNonNull(index, "index");

        Map<String, MavenModule> modulesByPom = new LinkedHashMap<>();
        modules.root().ifPresent(root -> collectModules(root, modulesByPom));
        Map<String, List<String>> pomsByCoordinate = coordinates(modulesByPom.values());
        List<JavaCompilationUnitFact> resolvedUnits = parsed.compilationUnits().stream()
                .map(unit -> resolveUnit(
                        unit,
                        index,
                        visibleModules(unit.sourceFile().modulePomFileId(), modulesByPom, pomsByCoordinate)))
                .toList();
        Set<Diagnostic> diagnostics = new LinkedHashSet<>(parsed.diagnostics());
        diagnostics.addAll(index.diagnostics());
        return new JavaParseAnalysis(resolvedUnits, List.copyOf(diagnostics));
    }

    private static JavaCompilationUnitFact resolveUnit(
            JavaCompilationUnitFact unit,
            JavaDeclarationIndex index,
            Set<String> visibleModules) {
        List<JavaTypeReferenceFact> references = unit.typeReferences().stream()
                .map(reference -> resolveReference(reference, unit, index, visibleModules))
                .toList();
        return new JavaCompilationUnitFact(
                unit.sourceFile(), unit.packageName(), unit.imports(), unit.types(), references);
    }

    private static JavaTypeReferenceFact resolveReference(
            JavaTypeReferenceFact reference,
            JavaCompilationUnitFact unit,
            JavaDeclarationIndex index,
            Set<String> visibleModules) {
        String name = reference.name();

        Resolution exact = qualifiedResolution(name, unit, index, visibleModules);
        if (exact.declared()) {
            return apply(reference, exact);
        }

        List<String> explicitCandidates = new ArrayList<>();
        boolean hasMatchingExplicitImport = false;
        for (JavaImportFact importFact : unit.imports()) {
            if (importFact.wildcard()) {
                continue;
            }
            String importedSimpleName = simpleName(importFact.name());
            if (name.equals(importedSimpleName) || name.startsWith(importedSimpleName + ".")) {
                hasMatchingExplicitImport = true;
                explicitCandidates.add(importFact.name() + name.substring(importedSimpleName.length()));
            }
        }
        Resolution explicit = resolution(explicitCandidates, index, visibleModules);
        if (hasMatchingExplicitImport) {
            return apply(reference, explicit);
        }

        if (unit.packageName().isPresent()) {
            Resolution samePackage = resolution(
                    List.of(unit.packageName().orElseThrow() + "." + name),
                    index,
                    visibleModules);
            if (samePackage.visibleDeclarations() > 0) {
                return apply(reference, samePackage);
            }
        }

        List<String> wildcardCandidates = unit.imports().stream()
                .filter(JavaImportFact::wildcard)
                .map(importFact -> importFact.name() + "." + name)
                .toList();
        return apply(reference, resolution(wildcardCandidates, index, visibleModules));
    }

    private static JavaTypeReferenceFact apply(JavaTypeReferenceFact reference, Resolution resolution) {
        if (resolution.visibleQualifiedNames().size() == 1 && resolution.visibleDeclarations() == 1) {
            return reference.resolved(resolution.visibleQualifiedNames().iterator().next());
        }
        if (resolution.visibleQualifiedNames().size() > 1 || resolution.visibleDeclarations() > 1) {
            return reference.ambiguous();
        }
        return JavaTypeReferenceFact.unresolved(reference.name(), reference.location());
    }

    private static Resolution resolution(
            Collection<String> candidates,
            JavaDeclarationIndex index,
            Set<String> visibleModules) {
        boolean declared = false;
        int visibleDeclarations = 0;
        Set<String> visibleQualifiedNames = new LinkedHashSet<>();
        for (String candidate : new LinkedHashSet<>(candidates)) {
            List<JavaDeclarationEntry> matches = index.findQualifiedName(candidate);
            if (!matches.isEmpty()) {
                declared = true;
            }
            for (JavaDeclarationEntry declaration : matches) {
                if (visibleModules.contains(declaration.modulePomFileId())) {
                    visibleDeclarations++;
                    visibleQualifiedNames.add(declaration.qualifiedName());
                }
            }
        }
        return new Resolution(declared, visibleDeclarations, visibleQualifiedNames);
    }

    private static Resolution qualifiedResolution(
            String candidate,
            JavaCompilationUnitFact unit,
            JavaDeclarationIndex index,
            Set<String> visibleModules) {
        List<JavaDeclarationEntry> eligible = index.findQualifiedName(candidate).stream()
                .filter(entry -> entry.packageName().isPresent() || unit.packageName().isEmpty())
                .toList();
        int visibleDeclarations = 0;
        Set<String> visibleQualifiedNames = new LinkedHashSet<>();
        for (JavaDeclarationEntry declaration : eligible) {
            if (visibleModules.contains(declaration.modulePomFileId())) {
                visibleDeclarations++;
                visibleQualifiedNames.add(declaration.qualifiedName());
            }
        }
        return new Resolution(!eligible.isEmpty(), visibleDeclarations, visibleQualifiedNames);
    }

    private static Set<String> visibleModules(
            String requestingPom,
            Map<String, MavenModule> modulesByPom,
            Map<String, List<String>> pomsByCoordinate) {
        Set<String> visible = new LinkedHashSet<>();
        visible.add(requestingPom);
        MavenModule requestingModule = modulesByPom.get(requestingPom);
        if (requestingModule == null) {
            return visible;
        }
        for (MavenDependency dependency : requestingModule.dependencies()) {
            if (!isCompileVisible(dependency)) {
                continue;
            }
            coordinate(dependency).ifPresent(coordinate -> {
                List<String> matches = pomsByCoordinate.getOrDefault(coordinate, List.of());
                if (matches.size() == 1) {
                    visible.add(matches.getFirst());
                }
            });
        }
        return visible;
    }

    private static boolean isCompileVisible(MavenDependency dependency) {
        String scope = dependency.scope().resolvedValue().orElse("compile");
        return scope.equals("compile") || scope.equals("provided") || scope.equals("system");
    }

    private static Map<String, List<String>> coordinates(Collection<MavenModule> modules) {
        Map<String, List<String>> result = new HashMap<>();
        for (MavenModule module : modules) {
            coordinate(module).ifPresent(coordinate -> result
                    .computeIfAbsent(coordinate, ignored -> new ArrayList<>())
                    .add(module.pomFileId()));
        }
        return result;
    }

    private static Optional<String> coordinate(MavenModule module) {
        return module.metadata().groupId().resolvedValue().flatMap(group ->
                module.metadata().artifactId().resolvedValue().map(artifact -> group + ":" + artifact));
    }

    private static Optional<String> coordinate(MavenDependency dependency) {
        return dependency.groupId().resolvedValue().flatMap(group ->
                dependency.artifactId().resolvedValue().map(artifact -> group + ":" + artifact));
    }

    private static void collectModules(MavenModule module, Map<String, MavenModule> modules) {
        modules.put(module.pomFileId(), module);
        module.children().forEach(child -> collectModules(child, modules));
    }

    private static String simpleName(String qualifiedName) {
        int separator = qualifiedName.lastIndexOf('.');
        return separator < 0 ? qualifiedName : qualifiedName.substring(separator + 1);
    }

    private record Resolution(
            boolean declared,
            int visibleDeclarations,
            Set<String> visibleQualifiedNames) {
    }
}
