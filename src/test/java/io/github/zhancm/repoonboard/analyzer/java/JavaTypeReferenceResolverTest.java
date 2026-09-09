package io.github.zhancm.repoonboard.analyzer.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaTypeReferenceResolverTest {

    @TempDir
    Path root;

    @Test
    void extractsImportsAndResolvesOnlyConfirmedCandidatesByPrecedence() throws IOException {
        pom("", "app", "");
        java("src/main/java/demo/App.java", """
                package demo;
                import explicit.api.Explicit;
                import wildcard.one.*;
                import wildcard.two.*;
                class App {
                    Explicit explicitValue;
                    Local localValue;
                    fq.FullyQualified qualifiedValue;
                    Unique uniqueValue;
                    Duplicate ambiguousValue;
                    Missing missingValue;
                    java.util.List<String> externalValue;
                }
                """);
        java("src/main/java/demo/Local.java", "package demo; class Local {}\n");
        java("src/main/java/explicit/api/Explicit.java", "package explicit.api; class Explicit {}\n");
        java("src/main/java/fq/FullyQualified.java", "package fq; class FullyQualified {}\n");
        java("src/main/java/wildcard/one/Unique.java", "package wildcard.one; class Unique {}\n");
        java("src/main/java/wildcard/one/Duplicate.java", "package wildcard.one; class Duplicate {}\n");
        java("src/main/java/wildcard/two/Duplicate.java", "package wildcard.two; class Duplicate {}\n");
        java("src/main/java/String.java", "class String {}\n");

        JavaCompilationUnitFact app = resolved().compilationUnits().stream()
                .filter(unit -> unit.sourceFile().relativePath().endsWith("demo/App.java"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                List.of("explicit.api.Explicit", "wildcard.one", "wildcard.two"),
                app.imports().stream().map(JavaImportFact::name).toList());
        assertTrue(app.imports().stream().allMatch(importFact ->
                importFact.location().startLine().orElseThrow() >= 2));

        Map<String, JavaTypeReferenceFact> references = app.typeReferences().stream()
                .collect(Collectors.toMap(
                        JavaTypeReferenceFact::name,
                        Function.identity(),
                        (first, ignored) -> first));
        assertResolved(references, "Explicit", "explicit.api.Explicit");
        assertResolved(references, "Local", "demo.Local");
        assertResolved(references, "fq.FullyQualified", "fq.FullyQualified");
        assertResolved(references, "Unique", "wildcard.one.Unique");
        assertEquals(JavaTypeReferenceStatus.AMBIGUOUS, references.get("Duplicate").status());
        assertEquals(JavaTypeReferenceStatus.UNRESOLVED, references.get("Missing").status());
        assertEquals(JavaTypeReferenceStatus.UNRESOLVED, references.get("java.util.List").status());
        assertEquals(JavaTypeReferenceStatus.UNRESOLVED, references.get("String").status());
        assertTrue(!references.containsKey("java") && !references.containsKey("java.util"));
    }

    @Test
    void crossModuleResolutionRequiresConfirmedMavenVisibility() throws IOException {
        pom("", "root", "<packaging>pom</packaging><modules><module>app</module>"
                + "<module>library</module><module>hidden</module></modules>");
        pom("app", "app", "<dependencies><dependency><groupId>example</groupId>"
                + "<artifactId>library</artifactId><version>1</version></dependency></dependencies>");
        pom("library", "library", "");
        pom("hidden", "hidden", "");
        java("app/src/main/java/app/App.java", """
                package app;
                import shared.LibraryType;
                import hidden.HiddenType;
                class App { LibraryType library; HiddenType hidden; }
                """);
        java("library/src/main/java/shared/LibraryType.java",
                "package shared; public class LibraryType {}\n");
        java("hidden/src/main/java/hidden/HiddenType.java",
                "package hidden; public class HiddenType {}\n");

        JavaCompilationUnitFact app = resolved().compilationUnits().stream()
                .filter(unit -> unit.sourceFile().modulePomFileId().equals("app/pom.xml"))
                .findFirst()
                .orElseThrow();
        Map<String, JavaTypeReferenceFact> references = app.typeReferences().stream()
                .collect(Collectors.toMap(JavaTypeReferenceFact::name, Function.identity()));
        assertResolved(references, "LibraryType", "shared.LibraryType");
        assertEquals(JavaTypeReferenceStatus.UNRESOLVED, references.get("HiddenType").status());
    }

    @Test
    void runtimeAndTestScopedModulesAreNotCompileVisible() throws IOException {
        pom("", "root", "<packaging>pom</packaging><modules><module>app</module>"
                + "<module>runtime-lib</module><module>test-lib</module></modules>");
        pom("app", "app", "<dependencies>"
                + dependency("runtime-lib", "runtime")
                + dependency("test-lib", "test")
                + "</dependencies>");
        pom("runtime-lib", "runtime-lib", "");
        pom("test-lib", "test-lib", "");
        java("app/src/main/java/app/App.java", """
                package app;
                import runtime.RuntimeType;
                import testing.TestType;
                class App { RuntimeType runtimeType; TestType testType; }
                """);
        java("runtime-lib/src/main/java/runtime/RuntimeType.java",
                "package runtime; public class RuntimeType {}\n");
        java("test-lib/src/main/java/testing/TestType.java",
                "package testing; public class TestType {}\n");

        JavaCompilationUnitFact app = resolved().compilationUnits().stream()
                .filter(unit -> unit.sourceFile().modulePomFileId().equals("app/pom.xml"))
                .findFirst()
                .orElseThrow();
        assertTrue(app.typeReferences().stream()
                .filter(reference -> reference.name().equals("RuntimeType")
                        || reference.name().equals("TestType"))
                .allMatch(reference -> reference.status() == JavaTypeReferenceStatus.UNRESOLVED));
    }

    private JavaParseAnalysis resolved() {
        var moduleAnalysis = new MavenModuleAnalyzer().analyze(
                root,
                new MavenModelOptions(root.resolve("cache"), List.of(), 1_048_576));
        var sourceRoots = new JavaSourceRootDiscoverer().discover(root, moduleAnalysis);
        var files = new JavaFileDiscoverer().discover(root, sourceRoots);
        var parsed = new JavaSourceParser().parse(root, files);
        return new JavaTypeReferenceResolver().resolve(parsed, moduleAnalysis);
    }

    private static void assertResolved(
            Map<String, JavaTypeReferenceFact> references, String name, String qualifiedName) {
        JavaTypeReferenceFact reference = references.get(name);
        assertEquals(JavaTypeReferenceStatus.RESOLVED, reference.status());
        assertEquals(qualifiedName, reference.resolvedQualifiedName().orElseThrow());
    }

    private void pom(String directory, String artifactId, String body) throws IOException {
        Path target = directory.isEmpty() ? root : root.resolve(directory);
        Files.createDirectories(target);
        Files.writeString(target.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>example</groupId><artifactId>" + artifactId + "</artifactId>"
                + "<version>1</version>" + body + "</project>");
    }

    private static String dependency(String artifactId, String scope) {
        return "<dependency><groupId>example</groupId><artifactId>" + artifactId
                + "</artifactId><version>1</version><scope>" + scope + "</scope></dependency>";
    }

    private void java(String relativePath, String source) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }
}
