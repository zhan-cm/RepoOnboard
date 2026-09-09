package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.Evidence;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Combines class- and method-level Spring MVC mappings into HTTP endpoints. */
public final class SpringEndpointAnalyzer {

    private static final String STAGE = "SPRING_ENDPOINT";

    public SpringEndpointAnalysis analyze(SpringMappingAnalysis mappingAnalysis) {
        List<SpringEndpointFact> endpoints = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        Set<OwnerKey> owners = new LinkedHashSet<>();
        mappingAnalysis.mappings().forEach(mapping -> owners.add(
                new OwnerKey(mapping.modulePomFileId(), mapping.controllerQualifiedName())));

        for (OwnerKey owner : owners) {
            List<SpringMappingFact> typeMappings = mappingAnalysis.mappings().stream()
                    .filter(mapping -> owner.matches(mapping))
                    .filter(mapping -> mapping.level() == SpringMappingLevel.TYPE)
                    .toList();
            List<SpringMappingFact> methodMappings = mappingAnalysis.mappings().stream()
                    .filter(mapping -> owner.matches(mapping))
                    .filter(mapping -> mapping.level() == SpringMappingLevel.METHOD)
                    .toList();
            for (SpringMappingFact methodMapping : methodMappings) {
                if (typeMappings.isEmpty()) {
                    combine(Optional.empty(), methodMapping, endpoints, diagnostics);
                } else {
                    for (SpringMappingFact typeMapping : typeMappings) {
                        combine(Optional.of(typeMapping), methodMapping, endpoints, diagnostics);
                    }
                }
            }
        }
        endpoints.sort(Comparator.comparing(SpringEndpointFact::modulePomFileId)
                .thenComparing(SpringEndpointFact::controllerQualifiedName)
                .thenComparing(SpringEndpointFact::handlerMethod)
                .thenComparing(endpoint -> endpoint.path().orElse("\uffff"))
                .thenComparing(endpoint -> endpoint.httpMethod().name()));
        return new SpringEndpointAnalysis(endpoints, diagnostics);
    }

    private static void combine(
            Optional<SpringMappingFact> typeMapping,
            SpringMappingFact methodMapping,
            List<SpringEndpointFact> endpoints,
            List<Diagnostic> diagnostics) {
        List<SpringHttpMethod> methods = combineMethods(typeMapping, methodMapping);
        if (methods.isEmpty()) {
            diagnostics.add(diagnostic(
                    "SPRING_ENDPOINT_METHOD_CONDITIONS_INCOMPATIBLE",
                    methodMapping,
                    "Class- and method-level HTTP method conditions have no common value."));
            return;
        }

        SpringMappingConditions conditions = combineConditions(
                typeMapping.map(SpringMappingFact::conditions)
                        .orElseGet(SpringMappingConditions::empty),
                methodMapping.conditions());
        List<Evidence> evidence = new ArrayList<>();
        typeMapping.ifPresent(mapping -> evidence.addAll(mapping.evidence()));
        evidence.addAll(methodMapping.evidence());

        List<String> typePaths = typeMapping.map(SpringMappingFact::paths).orElse(List.of(""));
        for (SpringHttpMethod method : methods) {
            for (String typePath : typePaths) {
                for (String methodPath : methodMapping.paths()) {
                    endpoints.add(endpoint(
                            methodMapping,
                            method,
                            Optional.of(joinPaths(typePath, methodPath)),
                            false,
                            conditions,
                            evidence));
                }
            }
            boolean unresolvedPath = methodMapping.unresolvedPath()
                    || typeMapping.map(SpringMappingFact::unresolvedPath).orElse(false);
            if (unresolvedPath) {
                endpoints.add(endpoint(
                        methodMapping,
                        method,
                        Optional.empty(),
                        true,
                        conditions,
                        evidence));
            }
        }
    }

    private static SpringEndpointFact endpoint(
            SpringMappingFact methodMapping,
            SpringHttpMethod method,
            Optional<String> path,
            boolean unresolvedPath,
            SpringMappingConditions conditions,
            List<Evidence> evidence) {
        return new SpringEndpointFact(
                methodMapping.modulePomFileId(),
                methodMapping.controllerQualifiedName(),
                methodMapping.handlerMethod().orElseThrow(),
                method,
                path,
                unresolvedPath,
                conditions,
                methodMapping.declarationLocation(),
                evidence);
    }

    private static List<SpringHttpMethod> combineMethods(
            Optional<SpringMappingFact> typeMapping, SpringMappingFact methodMapping) {
        List<SpringHttpMethod> typeMethods = typeMapping
                .map(SpringMappingFact::httpMethods)
                .orElse(List.of(SpringHttpMethod.ANY));
        List<SpringHttpMethod> methodMethods = methodMapping.httpMethods();
        boolean unresolved = typeMethods.contains(SpringHttpMethod.UNRESOLVED)
                || methodMethods.contains(SpringHttpMethod.UNRESOLVED);
        Set<SpringHttpMethod> typeRestricted = restricted(typeMethods);
        Set<SpringHttpMethod> methodRestricted = restricted(methodMethods);
        Set<SpringHttpMethod> result = new LinkedHashSet<>();
        if (typeRestricted.isEmpty() && methodRestricted.isEmpty()) {
            result.add(SpringHttpMethod.ANY);
        } else if (typeRestricted.isEmpty()) {
            result.addAll(methodRestricted);
        } else if (methodRestricted.isEmpty()) {
            result.addAll(typeRestricted);
        } else {
            result.addAll(typeRestricted);
            result.retainAll(methodRestricted);
        }
        if (unresolved) {
            result.add(SpringHttpMethod.UNRESOLVED);
        }
        return List.copyOf(result);
    }

    private static Set<SpringHttpMethod> restricted(List<SpringHttpMethod> methods) {
        Set<SpringHttpMethod> result = new LinkedHashSet<>(methods);
        result.remove(SpringHttpMethod.ANY);
        result.remove(SpringHttpMethod.UNRESOLVED);
        return result;
    }

    private static SpringMappingConditions combineConditions(
            SpringMappingConditions type, SpringMappingConditions method) {
        return new SpringMappingConditions(
                combine(type.params(), method.params()),
                combine(type.headers(), method.headers()),
                combine(type.consumes(), method.consumes()),
                combine(type.produces(), method.produces()),
                type.unresolved() || method.unresolved());
    }

    private static List<String> combine(List<String> left, List<String> right) {
        Set<String> values = new LinkedHashSet<>(left);
        values.addAll(right);
        return List.copyOf(values);
    }

    private static String joinPaths(String typePath, String methodPath) {
        String left = typePath.trim();
        String right = methodPath.trim();
        if (left.isEmpty() && right.isEmpty()) {
            return "/";
        }
        if (left.isEmpty()) {
            return ensureLeadingSlash(right);
        }
        if (right.isEmpty()) {
            return ensureLeadingSlash(left);
        }
        String normalizedLeft = ensureLeadingSlash(left);
        if (normalizedLeft.endsWith("/") && right.startsWith("/")) {
            return normalizedLeft + right.substring(1);
        }
        if (!normalizedLeft.endsWith("/") && !right.startsWith("/")) {
            return normalizedLeft + "/" + right;
        }
        return normalizedLeft + right;
    }

    private static String ensureLeadingSlash(String value) {
        return value.startsWith("/") ? value : "/" + value;
    }

    private static Diagnostic diagnostic(String code, SpringMappingFact mapping, String message) {
        return new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(mapping.modulePomFileId()),
                Optional.of(mapping.location().sourceFileId()),
                Optional.of(mapping.location()),
                message);
    }

    private record OwnerKey(String modulePomFileId, String qualifiedName) {

        boolean matches(SpringMappingFact mapping) {
            return modulePomFileId.equals(mapping.modulePomFileId())
                    && qualifiedName.equals(mapping.controllerQualifiedName());
        }
    }
}
