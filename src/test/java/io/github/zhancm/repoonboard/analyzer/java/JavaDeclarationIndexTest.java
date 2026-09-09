package io.github.zhancm.repoonboard.analyzer.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalysis;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaDeclarationIndexTest {

    @TempDir
    Path root;

    @Test
    void indexesNestedAndSameNamedTypesWithoutSilentOverwrite() throws IOException {
        pom("", "app", "");
        java("src/main/java/a/Outer.java", "package a; class Outer { class Inner {} }\n");
        java("src/main/java/a/First.java", "package a; class Same {}\n");
        java("src/main/java/a/Second.java", "package a; class Same {}\n");
        java("src/main/java/b/Same.java", "package b; class Same {}\n");

        JavaDeclarationIndex index = JavaDeclarationIndex.build(parse(analyzeModules()));

        assertEquals(5, index.declarations().size());
        assertEquals(2, index.findQualifiedName("a.Same").size());
        assertEquals(3, index.findSimpleName("Same").size());
        JavaDeclarationEntry nested = index.findQualifiedName("a.Outer.Inner").getFirst();
        assertEquals("pom.xml", nested.modulePomFileId());
        assertEquals("src/main/java/a/Outer.java", nested.location().sourceFileId());
        assertTrue(nested.location().startLine().isPresent());
        assertEquals(AnalysisStatus.PARTIAL, index.status());
        assertTrue(index.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("JAVA_DECLARATION_DUPLICATE")));
    }

    @Test
    void sharedQualifiedNameResolvesOnlyToVisibleModuleAndExternalStaysUnresolved()
            throws IOException {
        pom("", "root", "<packaging>pom</packaging><modules><module>app</module>"
                + "<module>visible</module><module>hidden</module></modules>");
        pom("app", "app", "<dependencies><dependency><groupId>example</groupId>"
                + "<artifactId>visible</artifactId><version>1</version></dependency></dependencies>");
        pom("visible", "visible", "");
        pom("hidden", "hidden", "");
        java("app/src/main/java/app/App.java", """
                package app;
                import shared.Shared;
                class App { Shared shared; java.util.List<String> external; }
                """);
        java("visible/src/main/java/shared/Shared.java",
                "package shared; public class Shared {}\n");
        java("hidden/src/main/java/shared/Shared.java",
                "package shared; public class Shared {}\n");

        MavenModuleAnalysis modules = analyzeModules();
        JavaParseAnalysis parsed = parse(modules);
        JavaDeclarationIndex index = JavaDeclarationIndex.build(parsed);
        JavaParseAnalysis resolved = new JavaTypeReferenceResolver().resolve(parsed, modules, index);

        assertEquals(2, index.findQualifiedName("shared.Shared").size());
        assertEquals(AnalysisStatus.SUCCESS, index.status());
        assertTrue(index.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("JAVA_DECLARATION_NAME_SHARED_ACROSS_MODULES")));
        JavaCompilationUnitFact app = resolved.compilationUnits().stream()
                .filter(unit -> unit.sourceFile().modulePomFileId().equals("app/pom.xml"))
                .findFirst()
                .orElseThrow();
        JavaTypeReferenceFact shared = reference(app, "Shared");
        assertEquals(JavaTypeReferenceStatus.RESOLVED, shared.status());
        assertEquals("shared.Shared", shared.resolvedQualifiedName().orElseThrow());
        assertEquals(JavaTypeReferenceStatus.UNRESOLVED, reference(app, "java.util.List").status());
        assertEquals(JavaTypeReferenceStatus.UNRESOLVED, reference(app, "String").status());
    }

    private JavaTypeReferenceFact reference(JavaCompilationUnitFact unit, String name) {
        return unit.typeReferences().stream()
                .filter(reference -> reference.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private MavenModuleAnalysis analyzeModules() {
        return new MavenModuleAnalyzer().analyze(
                root,
                new MavenModelOptions(root.resolve("cache"), List.of(), 1_048_576));
    }

    private JavaParseAnalysis parse(MavenModuleAnalysis modules) {
        var sourceRoots = new JavaSourceRootDiscoverer().discover(root, modules);
        var files = new JavaFileDiscoverer().discover(root, sourceRoots);
        return new JavaSourceParser().parse(root, files);
    }

    private void pom(String directory, String artifactId, String body) throws IOException {
        Path target = directory.isEmpty() ? root : root.resolve(directory);
        Files.createDirectories(target);
        Files.writeString(target.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion>"
                + "<groupId>example</groupId><artifactId>" + artifactId + "</artifactId>"
                + "<version>1</version>" + body + "</project>");
    }

    private void java(String relativePath, String source) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }
}
