package io.github.zhancm.repoonboard.analyzer.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Parses Java source files into stable project-owned facts. */
public final class JavaSourceParser {

    private static final String STAGE = "JAVA_PARSE";

    private final JavaParser parser;

    public JavaSourceParser() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setCharacterEncoding(StandardCharsets.UTF_8)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.parser = new JavaParser(configuration);
    }

    public JavaParseAnalysis parse(Path scanRoot, JavaFileDiscovery discovery) {
        Objects.requireNonNull(discovery, "discovery");
        if (scanRoot == null || !Files.isDirectory(scanRoot)) {
            throw new IllegalArgumentException("scanRoot must be an existing directory");
        }

        List<JavaCompilationUnitFact> units = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        final Path realScanRoot;
        try {
            realScanRoot = scanRoot.toRealPath();
        } catch (IOException exception) {
            diagnostics.add(diagnostic(
                    "JAVA_PARSE_SCAN_ROOT_UNAVAILABLE",
                    Optional.empty(),
                    "pom.xml",
                    "The Java parse scan root could not be resolved safely."));
            return new JavaParseAnalysis(List.of(), diagnostics);
        }

        for (JavaSourceFile sourceFile : discovery.files()) {
            parseFile(realScanRoot, sourceFile, units, diagnostics);
        }
        units.sort(Comparator.comparing(unit -> unit.sourceFile().relativePath()));
        return new JavaParseAnalysis(units, diagnostics);
    }

    private void parseFile(
            Path scanRoot,
            JavaSourceFile sourceFile,
            List<JavaCompilationUnitFact> units,
            List<Diagnostic> diagnostics) {
        Path configuredFile = scanRoot.resolve(sourceFile.relativePath()).normalize();
        try {
            Path realFile = configuredFile.toRealPath();
            if (!realFile.startsWith(scanRoot) || !Files.isRegularFile(realFile)) {
                diagnostics.add(diagnostic(
                        "JAVA_PARSE_FILE_OUTSIDE_SCAN",
                        Optional.of(sourceFile.modulePomFileId()),
                        sourceFile.relativePath(),
                        "A Java source file no longer resolves to a regular file inside the scan root."));
                return;
            }

            ParseResult<CompilationUnit> result = parser.parse(realFile);
            if (!result.getProblems().isEmpty()) {
                diagnostics.add(diagnostic(
                        "JAVA_PARSE_PROBLEM",
                        Optional.of(sourceFile.modulePomFileId()),
                        sourceFile.relativePath(),
                        "JavaParser reported one or more syntax problems in this source file."));
            }
            result.getResult().ifPresent(unit -> units.add(toFacts(sourceFile, unit)));
        } catch (IOException | RuntimeException exception) {
            diagnostics.add(diagnostic(
                    "JAVA_PARSE_FILE_FAILED",
                    Optional.of(sourceFile.modulePomFileId()),
                    sourceFile.relativePath(),
                    "The Java source file could not be parsed; analysis continued with other files."));
        }
    }

    private static JavaCompilationUnitFact toFacts(JavaSourceFile sourceFile, CompilationUnit unit) {
        Optional<String> packageName = unit.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString());
        List<JavaImportFact> imports = unit.getImports().stream()
                .map(declaration -> importFact(sourceFile, declaration))
                .toList();
        List<JavaTypeFact> types = new ArrayList<>();
        for (TypeDeclaration<?> declaration : unit.getTypes()) {
            collectType(sourceFile, packageName.orElse(""), Optional.empty(), declaration, types);
        }
        types.sort(Comparator.comparing(JavaTypeFact::qualifiedName)
                .thenComparingInt(type -> type.location().startLine().orElse(Integer.MAX_VALUE)));
        List<JavaTypeReferenceFact> references = new ArrayList<>();
        unit.findAll(ClassOrInterfaceType.class).stream()
                .filter(type -> !isScopeSegment(type))
                .forEach(type -> references.add(
                JavaTypeReferenceFact.unresolved(
                        type.getNameWithScope(),
                        location(sourceFile, type, type.getNameWithScope()))));
        unit.findAll(AnnotationExpr.class).forEach(annotation -> references.add(
                JavaTypeReferenceFact.unresolved(
                        annotation.getNameAsString(),
                        location(sourceFile, annotation, annotation.getNameAsString()))));
        references.sort(Comparator.<JavaTypeReferenceFact>comparingInt(
                        reference -> reference.location().startLine().orElse(Integer.MAX_VALUE))
                .thenComparingInt(reference -> reference.location().startColumn().orElse(Integer.MAX_VALUE))
                .thenComparing(JavaTypeReferenceFact::name));
        return new JavaCompilationUnitFact(sourceFile, packageName, imports, types, references);
    }

    private static boolean isScopeSegment(ClassOrInterfaceType type) {
        return type.getParentNode()
                .filter(ClassOrInterfaceType.class::isInstance)
                .map(ClassOrInterfaceType.class::cast)
                .flatMap(ClassOrInterfaceType::getScope)
                .filter(scope -> scope == type)
                .isPresent();
    }

    private static JavaImportFact importFact(JavaSourceFile sourceFile, ImportDeclaration declaration) {
        return new JavaImportFact(
                declaration.getNameAsString(),
                declaration.isStatic(),
                declaration.isAsterisk(),
                location(sourceFile, declaration, declaration.getNameAsString()));
    }

    private static void collectType(
            JavaSourceFile sourceFile,
            String packageName,
            Optional<String> enclosingType,
            TypeDeclaration<?> declaration,
            List<JavaTypeFact> types) {
        String simpleName = declaration.getNameAsString();
        String qualifiedName = enclosingType
                .map(parent -> parent + "." + simpleName)
                .orElseGet(() -> packageName.isEmpty() ? simpleName : packageName + "." + simpleName);

        List<JavaFieldFact> fields = new ArrayList<>();
        List<JavaMethodFact> methods = new ArrayList<>();
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof FieldDeclaration field) {
                List<JavaAnnotationFact> annotations = annotations(sourceFile, field);
                field.getVariables().forEach(variable -> fields.add(new JavaFieldFact(
                        variable.getNameAsString(),
                        variable.getType().toString(),
                        annotations,
                        location(sourceFile, variable, qualifiedName + "#" + variable.getNameAsString()))));
            } else if (member instanceof MethodDeclaration method) {
                List<JavaParameterFact> parameters = method.getParameters().stream()
                        .map(parameter -> new JavaParameterFact(
                                parameter.getNameAsString(),
                                parameter.getType().toString(),
                                location(sourceFile, parameter, qualifiedName + "#" + method.getNameAsString())))
                        .toList();
                methods.add(new JavaMethodFact(
                        method.getNameAsString(),
                        method.getType().toString(),
                        parameters,
                        annotations(sourceFile, method),
                        location(sourceFile, method, qualifiedName + "#" + method.getNameAsString())));
            }
        }
        fields.sort(Comparator.comparingInt(field -> field.location().startLine().orElse(Integer.MAX_VALUE)));
        methods.sort(Comparator.comparingInt(method -> method.location().startLine().orElse(Integer.MAX_VALUE)));
        types.add(new JavaTypeFact(
                simpleName,
                qualifiedName,
                enclosingType,
                kind(declaration),
                annotations(sourceFile, declaration),
                fields,
                methods,
                location(sourceFile, declaration, qualifiedName)));

        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                collectType(sourceFile, packageName, Optional.of(qualifiedName), nested, types);
            }
        }
    }

    private static JavaTypeKind kind(TypeDeclaration<?> declaration) {
        if (declaration instanceof AnnotationDeclaration) {
            return JavaTypeKind.ANNOTATION;
        }
        if (declaration instanceof ClassOrInterfaceDeclaration classOrInterface) {
            return classOrInterface.isInterface() ? JavaTypeKind.INTERFACE : JavaTypeKind.CLASS;
        }
        if (declaration instanceof EnumDeclaration) {
            return JavaTypeKind.ENUM;
        }
        if (declaration instanceof RecordDeclaration) {
            return JavaTypeKind.RECORD;
        }
        throw new IllegalArgumentException("Unsupported Java type declaration: " + declaration.getClass().getName());
    }

    private static List<JavaAnnotationFact> annotations(
            JavaSourceFile sourceFile, NodeWithAnnotations<?> annotated) {
        return annotated.getAnnotations().stream()
                .map(annotation -> annotationFact(sourceFile, annotation))
                .toList();
    }

    private static JavaAnnotationFact annotationFact(
            JavaSourceFile sourceFile, AnnotationExpr annotation) {
        List<JavaAnnotationAttributeFact> attributes = new ArrayList<>();
        if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
            attributes.add(annotationAttribute(sourceFile, "value", singleMember.getMemberValue()));
        } else if (annotation instanceof NormalAnnotationExpr normal) {
            normal.getPairs().forEach(pair -> attributes.add(annotationAttribute(
                    sourceFile, pair.getNameAsString(), pair.getValue())));
        }
        return new JavaAnnotationFact(
                annotation.getNameAsString(),
                annotation.toString(),
                attributes,
                location(sourceFile, annotation, annotation.getNameAsString()));
    }

    private static JavaAnnotationAttributeFact annotationAttribute(
            JavaSourceFile sourceFile, String name, Expression expression) {
        return new JavaAnnotationAttributeFact(
                name,
                expression.toString(),
                stringLiterals(expression),
                location(sourceFile, expression, name));
    }

    private static List<String> stringLiterals(Expression expression) {
        if (expression.isStringLiteralExpr()) {
            return List.of(expression.asStringLiteralExpr().asString());
        }
        if (expression instanceof ArrayInitializerExpr array
                && array.getValues().stream().allMatch(Expression::isStringLiteralExpr)) {
            return array.getValues().stream()
                    .map(value -> value.asStringLiteralExpr().asString())
                    .toList();
        }
        return List.of();
    }

    private static SourceLocation location(JavaSourceFile sourceFile, Node node, String symbol) {
        Optional<Range> range = node.getRange();
        if (range.isEmpty()) {
            return new SourceLocation(
                    sourceFile.relativePath(),
                    OptionalInt.empty(),
                    OptionalInt.empty(),
                    OptionalInt.empty(),
                    OptionalInt.empty(),
                    Optional.of(symbol));
        }
        Position begin = range.orElseThrow().begin;
        Position end = range.orElseThrow().end;
        return new SourceLocation(
                sourceFile.relativePath(),
                OptionalInt.of(begin.line),
                OptionalInt.of(begin.column),
                OptionalInt.of(end.line),
                OptionalInt.of(end.column),
                Optional.of(symbol));
    }

    private static Diagnostic diagnostic(
            String code, Optional<String> moduleId, String fileId, String message) {
        return new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                moduleId,
                Optional.of(fileId),
                Optional.of(SourceLocation.file(fileId)),
                message);
    }
}
