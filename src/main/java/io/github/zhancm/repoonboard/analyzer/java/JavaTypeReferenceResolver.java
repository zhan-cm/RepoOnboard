package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.analyzer.maven.MavenDependency;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalysis;
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
        Objects.requireNonNull(parsed, "parsed");
        Objects.requireNonNull(modules, "modules");

        Map<String, MavenModule> modulesByPom = new LinkedHashMap<>();
        modules.root().ifPresent(root -> collectModules(root, modulesByPom));
        Map<String, List<String>> pomsByCoordinate = coordinates(modulesByPom.values());
        Map<String, List<Declaration>> declarations = declarations(parsed);

        List<JavaCompilationUnitFact> resolvedUnits = parsed.compilationUnits().stream()
                .map(unit -> resolveUnit(
                        unit,
                        declarations,
                        visibleModules(unit.sourceFile().modulePomFileId(), modulesByPom, pomsByCoordinate)))
                .toList();
        return new JavaParseAnalysis(resolvedUnits, parsed.diagnostics());
    }

    private static JavaCompilationUnitFact resolveUnit(
            JavaCompilationUnitFact unit,
            Map<String, List<Declaration>> declarations,
            Set<String> visibleModules) {
        List<JavaTypeReferenceFact> references = unit.typeReferences().stream()
                .map(reference -> resolveReference(reference, unit, declarations, visibleModules))
                .toList();
        return new JavaCompilationUnitFact(
                unit.sourceFile(), unit.packageName(), unit.imports(), unit.types(), references);
    }

    private static JavaTypeReferenceFact resolveReference(
            JavaTypeReferenceFact reference,
            JavaCompilationUnitFact unit,
            Map<String, List<Declaration>> declarations,
            Set<String> visibleModules) {
        String name = reference.name();

        Resolution exact = resolution(List.of(name), declarations, visibleModules);
        if (exact.declared()) {
            return apply(reference, exact);
        }

        List<String> explicitCandidates = new ArrayList<>();
        boolean hasMatchingExplicitImport = false;
        for (JavaImportFact importFact : unit.imports()) {
            if (importFact.staticImport() || importFact.wildcard()) {
                continue;
            }
            String importedSimpleName = simpleName(importFact.name());
            if (name.equals(importedSimpleName) || name.startsWith(importedSimpleName + ".")) {
                hasMatchingExplicitImport = true;
                explicitCandidates.add(importFact.name() + name.substring(importedSimpleName.length()));
            }
        }
        Resolution explicit = resolution(explicitCandidates, declarations, visibleModules);
        if (hasMatchingExplicitImport) {
            return apply(reference, explicit);
        }

        if (unit.packageName().isPresent()) {
            Resolution samePackage = resolution(
                    List.of(unit.packageName().orElseThrow() + "." + name),
                    declarations,
                    visibleModules);
            if (samePackage.declared()) {
                return apply(reference, samePackage);
            }
        }

        List<String> wildcardCandidates = unit.imports().stream()
                .filter(importFact -> !importFact.staticImport() && importFact.wildcard())
                .map(importFact -> importFact.name() + "." + name)
                .toList();
        return apply(reference, resolution(wildcardCandidates, declarations, visibleModules));
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
            Map<String, List<Declaration>> declarations,
            Set<String> visibleModules) {
        boolean declared = false;
        int visibleDeclarations = 0;
        Set<String> visibleQualifiedNames = new LinkedHashSet<>();
        for (String candidate : new LinkedHashSet<>(candidates)) {
            List<Declaration> matches = declarations.getOrDefault(candidate, List.of());
            if (!matches.isEmpty()) {
                declared = true;
            }
            for (Declaration declaration : matches) {
                if (visibleModules.contains(declaration.modulePomFileId())) {
                    visibleDeclarations++;
                    visibleQualifiedNames.add(declaration.qualifiedName());
                }
            }
        }
        return new Resolution(declared, visibleDeclarations, visibleQualifiedNames);
    }

    private static Map<String, List<Declaration>> declarations(JavaParseAnalysis parsed) {
        Map<String, List<Declaration>> declarations = new LinkedHashMap<>();
        for (JavaCompilationUnitFact unit : parsed.compilationUnits()) {
            for (JavaTypeFact type : unit.types()) {
                declarations.computeIfAbsent(type.qualifiedName(), ignored -> new ArrayList<>())
                        .add(new Declaration(unit.sourceFile().modulePomFileId(), type.qualifiedName()));
            }
        }
        return declarations;
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

    private record Declaration(String modulePomFileId, String qualifiedName) {
    }

    private record Resolution(
            boolean declared,
            int visibleDeclarations,
            Set<String> visibleQualifiedNames) {
    }
}
