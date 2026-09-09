package io.github.zhancm.repoonboard.analyzer.java;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Module-aware qualified-name and simple-name index of project declarations. */
public final class JavaDeclarationIndex {

    private static final String STAGE = "JAVA_DECLARATION_INDEX";

    private final List<JavaDeclarationEntry> declarations;
    private final Map<String, List<JavaDeclarationEntry>> byQualifiedName;
    private final Map<String, List<JavaDeclarationEntry>> bySimpleName;
    private final List<Diagnostic> diagnostics;

    private JavaDeclarationIndex(
            List<JavaDeclarationEntry> declarations,
            Map<String, List<JavaDeclarationEntry>> byQualifiedName,
            Map<String, List<JavaDeclarationEntry>> bySimpleName,
            List<Diagnostic> diagnostics) {
        this.declarations = List.copyOf(declarations);
        this.byQualifiedName = immutableLists(byQualifiedName);
        this.bySimpleName = immutableLists(bySimpleName);
        this.diagnostics = List.copyOf(diagnostics);
    }

    public static JavaDeclarationIndex build(JavaParseAnalysis analysis) {
        Objects.requireNonNull(analysis, "analysis");
        List<JavaDeclarationEntry> declarations = new ArrayList<>();
        for (JavaCompilationUnitFact unit : analysis.compilationUnits()) {
            for (JavaTypeFact type : unit.types()) {
                declarations.add(new JavaDeclarationEntry(
                        unit.sourceFile().modulePomFileId(),
                        unit.packageName(),
                        type.qualifiedName(),
                        type.simpleName(),
                        type.kind(),
                        type.location()));
            }
        }
        declarations.sort(Comparator.comparing(JavaDeclarationEntry::qualifiedName)
                .thenComparing(JavaDeclarationEntry::modulePomFileId)
                .thenComparing(entry -> entry.location().sourceFileId())
                .thenComparingInt(entry -> entry.location().startLine().orElse(Integer.MAX_VALUE)));

        Map<String, List<JavaDeclarationEntry>> qualified = group(
                declarations, JavaDeclarationEntry::qualifiedName);
        Map<String, List<JavaDeclarationEntry>> simple = group(
                declarations, JavaDeclarationEntry::simpleName);
        return new JavaDeclarationIndex(
                declarations,
                qualified,
                simple,
                duplicateDiagnostics(qualified));
    }

    public List<JavaDeclarationEntry> declarations() {
        return declarations;
    }

    public List<JavaDeclarationEntry> findQualifiedName(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return byQualifiedName.getOrDefault(qualifiedName, List.of());
    }

    public List<JavaDeclarationEntry> findSimpleName(String simpleName) {
        Objects.requireNonNull(simpleName, "simpleName");
        return bySimpleName.getOrDefault(simpleName, List.of());
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    public AnalysisStatus status() {
        return AnalysisStatus.fromDiagnostics(diagnostics);
    }

    public long ambiguousQualifiedNameCount() {
        return byQualifiedName.values().stream().filter(entries -> entries.size() > 1).count();
    }

    private static Map<String, List<JavaDeclarationEntry>> group(
            List<JavaDeclarationEntry> declarations,
            java.util.function.Function<JavaDeclarationEntry, String> key) {
        Map<String, List<JavaDeclarationEntry>> grouped = new LinkedHashMap<>();
        for (JavaDeclarationEntry declaration : declarations) {
            grouped.computeIfAbsent(key.apply(declaration), ignored -> new ArrayList<>())
                    .add(declaration);
        }
        return grouped;
    }

    private static List<Diagnostic> duplicateDiagnostics(
            Map<String, List<JavaDeclarationEntry>> qualified) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (Map.Entry<String, List<JavaDeclarationEntry>> group : qualified.entrySet()) {
            if (group.getValue().size() < 2) {
                continue;
            }
            JavaDeclarationEntry first = group.getValue().getFirst();
            boolean duplicateWithinModule = group.getValue().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            JavaDeclarationEntry::modulePomFileId,
                            java.util.stream.Collectors.counting()))
                    .values().stream()
                    .anyMatch(count -> count > 1);
            DiagnosticSeverity severity = duplicateWithinModule
                    ? DiagnosticSeverity.WARNING
                    : DiagnosticSeverity.INFO;
            String code = duplicateWithinModule
                    ? "JAVA_DECLARATION_DUPLICATE"
                    : "JAVA_DECLARATION_NAME_SHARED_ACROSS_MODULES";
            diagnostics.add(new Diagnostic(
                    code,
                    severity,
                    STAGE,
                    Optional.of(first.modulePomFileId()),
                    Optional.of(first.location().sourceFileId()),
                    Optional.of(first.location()),
                    "Multiple Java declarations share the qualified name " + group.getKey()
                            + "; all candidates remain indexed."));
        }
        return diagnostics;
    }

    private static Map<String, List<JavaDeclarationEntry>> immutableLists(
            Map<String, List<JavaDeclarationEntry>> source) {
        Map<String, List<JavaDeclarationEntry>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(copy);
    }
}
