package io.github.zhancm.repoonboard.analyzer.spring;

import io.github.zhancm.repoonboard.analyzer.java.JavaAnnotationAttributeFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaAnnotationFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaCompilationUnitFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaMethodFact;
import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeFact;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Extracts confirmed standard Spring MVC mapping declarations from controllers. */
public final class SpringMvcMappingAnalyzer {

    private static final String STAGE = "SPRING_MVC_MAPPING";
    private static final String REQUEST_MAPPING =
            "org.springframework.web.bind.annotation.RequestMapping";
    private static final Map<String, SpringHttpMethod> MAPPING_ANNOTATIONS = Map.of(
            REQUEST_MAPPING, SpringHttpMethod.ANY,
            "org.springframework.web.bind.annotation.GetMapping", SpringHttpMethod.GET,
            "org.springframework.web.bind.annotation.PostMapping", SpringHttpMethod.POST,
            "org.springframework.web.bind.annotation.PutMapping", SpringHttpMethod.PUT,
            "org.springframework.web.bind.annotation.DeleteMapping", SpringHttpMethod.DELETE,
            "org.springframework.web.bind.annotation.PatchMapping", SpringHttpMethod.PATCH);
    private static final Set<String> CONDITION_NAMES = Set.of(
            "params", "headers", "consumes", "produces");

    public SpringMappingAnalysis analyze(
            JavaParseAnalysis javaAnalysis, SpringComponentAnalysis componentAnalysis) {
        Set<OwnerKey> controllers = new LinkedHashSet<>();
        componentAnalysis.components().stream()
                .filter(component -> component.kind() == SpringComponentKind.CONTROLLER
                        || component.kind() == SpringComponentKind.REST_CONTROLLER)
                .forEach(component -> controllers.add(
                        new OwnerKey(component.modulePomFileId(), component.qualifiedName())));

        List<SpringMappingFact> mappings = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (JavaCompilationUnitFact unit : javaAnalysis.compilationUnits()) {
            for (JavaTypeFact type : unit.types()) {
                if (!controllers.contains(new OwnerKey(
                        unit.sourceFile().modulePomFileId(), type.qualifiedName()))) {
                    continue;
                }
                extract(unit, type, Optional.empty(), type.annotations(), type.location(), mappings, diagnostics);
                for (JavaMethodFact method : type.methods()) {
                    extract(unit, type, Optional.of(method.name()), method.annotations(),
                            method.location(), mappings, diagnostics);
                }
            }
        }
        mappings.sort(Comparator.comparing(SpringMappingFact::modulePomFileId)
                .thenComparing(SpringMappingFact::controllerQualifiedName)
                .thenComparingInt(mapping -> mapping.location().startLine().orElse(Integer.MAX_VALUE))
                .thenComparing(mapping -> mapping.handlerMethod().orElse("")));
        return new SpringMappingAnalysis(mappings, diagnostics);
    }

    private static void extract(
            JavaCompilationUnitFact unit,
            JavaTypeFact owner,
            Optional<String> handlerMethod,
            List<JavaAnnotationFact> annotations,
            SourceLocation declarationLocation,
            List<SpringMappingFact> mappings,
            List<Diagnostic> diagnostics) {
        int confirmedCount = 0;
        for (JavaAnnotationFact annotation : annotations) {
            SpringAnnotationResolution resolution = SpringAnnotationMatcher.resolve(
                    annotation, unit, MAPPING_ANNOTATIONS.keySet());
            if (resolution.status() == SpringAnnotationResolution.Status.AMBIGUOUS) {
                diagnostics.add(diagnostic(
                        "SPRING_MVC_MAPPING_ANNOTATION_AMBIGUOUS",
                        unit,
                        annotation.location(),
                        "A Spring MVC mapping annotation could not be confirmed because its imports are ambiguous."));
                continue;
            }
            if (resolution.status() != SpringAnnotationResolution.Status.CONFIRMED) {
                continue;
            }
            confirmedCount++;
            mappings.add(mapping(
                    unit,
                    owner,
                    handlerMethod,
                    declarationLocation,
                    annotation,
                    resolution.qualifiedName().orElseThrow(),
                    diagnostics));
        }
        if (confirmedCount > 1) {
            diagnostics.add(diagnostic(
                    "SPRING_MVC_MULTIPLE_MAPPING_ANNOTATIONS",
                    unit,
                    declarationLocation,
                    "Multiple Spring MVC mapping annotations are declared on the same element; each is retained."));
        }
    }

    private static SpringMappingFact mapping(
            JavaCompilationUnitFact unit,
            JavaTypeFact owner,
            Optional<String> handlerMethod,
            SourceLocation declarationLocation,
            JavaAnnotationFact annotation,
            String annotationQualifiedName,
            List<Diagnostic> diagnostics) {
        LiteralValues pathValues = values(annotation, Set.of("value", "path"), true);
        if (pathValues.unresolved()) {
            diagnostics.add(diagnostic(
                    "SPRING_MVC_PATH_UNRESOLVED",
                    unit,
                    annotation.location(),
                    "A mapping path contains an expression that is not a string literal."));
        }

        MethodValues methodValues = annotationQualifiedName.equals(REQUEST_MAPPING)
                ? requestMethods(annotation)
                : new MethodValues(List.of(MAPPING_ANNOTATIONS.get(annotationQualifiedName)), false);
        if (methodValues.unresolved()) {
            diagnostics.add(diagnostic(
                    "SPRING_MVC_METHOD_UNRESOLVED",
                    unit,
                    annotation.location(),
                    "A RequestMapping method condition could not be fully resolved."));
        }

        SpringMappingConditions conditions = conditions(annotation);
        if (conditions.unresolved()) {
            diagnostics.add(diagnostic(
                    "SPRING_MVC_CONDITION_UNRESOLVED",
                    unit,
                    annotation.location(),
                    "One or more mapping conditions are not string literals and remain unresolved."));
        }
        SpringMappingLevel level = handlerMethod.isPresent()
                ? SpringMappingLevel.METHOD
                : SpringMappingLevel.TYPE;
        return new SpringMappingFact(
                unit.sourceFile().modulePomFileId(),
                owner.qualifiedName(),
                handlerMethod,
                level,
                methodValues.methods(),
                methodValues.unresolved(),
                pathValues.values(),
                pathValues.unresolved(),
                conditions,
                annotation.location(),
                List.of(new Evidence(
                        level == SpringMappingLevel.TYPE
                                ? "SPRING_MVC_TYPE_MAPPING"
                                : "SPRING_MVC_METHOD_MAPPING",
                        annotation.location(),
                        List.of(declarationLocation),
                        "spring.mvc.mapping.annotation:" + annotationQualifiedName)));
    }

    private static MethodValues requestMethods(JavaAnnotationFact annotation) {
        Optional<JavaAnnotationAttributeFact> method = annotation.attributes().stream()
                .filter(attribute -> attribute.name().equals("method"))
                .findFirst();
        if (method.isEmpty()) {
            return new MethodValues(List.of(SpringHttpMethod.ANY), false);
        }
        String expression = method.orElseThrow().expression()
                .replace("{", "")
                .replace("}", "")
                .trim();
        if (expression.isEmpty()) {
            return new MethodValues(List.of(SpringHttpMethod.ANY), false);
        }
        List<SpringHttpMethod> methods = new ArrayList<>();
        boolean unresolved = false;
        for (String rawToken : expression.split(",")) {
            String token = rawToken.trim();
            int separator = token.lastIndexOf('.');
            String name = separator < 0 ? token : token.substring(separator + 1);
            try {
                SpringHttpMethod parsed = SpringHttpMethod.valueOf(name);
                if (parsed == SpringHttpMethod.ANY || parsed == SpringHttpMethod.UNRESOLVED) {
                    unresolved = true;
                } else {
                    methods.add(parsed);
                }
            } catch (IllegalArgumentException ignored) {
                unresolved = true;
            }
        }
        if (unresolved) {
            methods.add(SpringHttpMethod.UNRESOLVED);
        }
        return new MethodValues(distinct(methods), unresolved);
    }

    private static SpringMappingConditions conditions(JavaAnnotationFact annotation) {
        Map<String, LiteralValues> values = new LinkedHashMap<>();
        for (String name : CONDITION_NAMES) {
            values.put(name, values(annotation, Set.of(name), false));
        }
        return new SpringMappingConditions(
                values.get("params").values(),
                values.get("headers").values(),
                values.get("consumes").values(),
                values.get("produces").values(),
                values.values().stream().anyMatch(LiteralValues::unresolved));
    }

    private static LiteralValues values(
            JavaAnnotationFact annotation, Set<String> names, boolean defaultEmptyPath) {
        List<JavaAnnotationAttributeFact> attributes = annotation.attributes().stream()
                .filter(attribute -> names.contains(attribute.name()))
                .toList();
        if (attributes.isEmpty()) {
            return new LiteralValues(defaultEmptyPath ? List.of("") : List.of(), false);
        }
        List<String> literals = new ArrayList<>();
        boolean unresolved = false;
        for (JavaAnnotationAttributeFact attribute : attributes) {
            literals.addAll(attribute.stringLiterals());
            unresolved |= !containsOnlyStringLiterals(attribute.expression());
        }
        if (literals.isEmpty() && !unresolved && defaultEmptyPath) {
            literals.add("");
        }
        return new LiteralValues(distinct(literals), unresolved);
    }

    private static boolean containsOnlyStringLiterals(String expression) {
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < expression.length(); index++) {
            char current = expression.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
            } else if (current == '"') {
                inString = true;
            } else if (!Character.isWhitespace(current)
                    && current != '{' && current != '}' && current != ',') {
                return false;
            }
        }
        return !inString;
    }

    private static <T> List<T> distinct(List<T> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private static Diagnostic diagnostic(
            String code, JavaCompilationUnitFact unit, SourceLocation location, String message) {
        return new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.of(unit.sourceFile().modulePomFileId()),
                Optional.of(location.sourceFileId()),
                Optional.of(location),
                message);
    }

    private record OwnerKey(String modulePomFileId, String qualifiedName) {
    }

    private record LiteralValues(List<String> values, boolean unresolved) {
    }

    private record MethodValues(List<SpringHttpMethod> methods, boolean unresolved) {

        MethodValues {
            if (methods.isEmpty() && !unresolved) {
                throw new IllegalArgumentException("Resolved method set must not be empty");
            }
        }
    }
}
