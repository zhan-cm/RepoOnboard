package io.github.zhancm.repoonboard.analyzer.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import io.github.zhancm.repoonboard.testing.FixturePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaSourceParserTest {

    @TempDir
    Path scanRoot;

    @Test
    void parsesJavaEightThroughTwentyOneFixtureIntoProjectFacts() {
        Path fixture = FixturePaths.project("java-syntax-project");
        JavaFileDiscovery files = new JavaFileDiscoverer().discover(
                fixture,
                new JavaSourceRootDiscovery(
                        List.of(new JavaSourceRoot(
                                "pom.xml", ".", "src/main/java", SourceLocation.file("pom.xml"))),
                        List.of()));

        JavaParseAnalysis analysis = new JavaSourceParser().parse(fixture, files);

        assertEquals(AnalysisStatus.SUCCESS, analysis.status());
        assertEquals(4, analysis.compilationUnits().size());
        List<JavaTypeFact> types = analysis.compilationUnits().stream()
                .flatMap(unit -> unit.types().stream())
                .toList();
        assertTrue(types.stream().anyMatch(type -> type.kind() == JavaTypeKind.CLASS));
        assertTrue(types.stream().anyMatch(type -> type.kind() == JavaTypeKind.INTERFACE));
        assertTrue(types.stream().anyMatch(type -> type.kind() == JavaTypeKind.ENUM));
        assertTrue(types.stream().anyMatch(type -> type.kind() == JavaTypeKind.RECORD));
        assertTrue(types.stream().anyMatch(type -> type.qualifiedName().equals("syntax.Java8Style.Nested")));

        JavaTypeFact javaEight = types.stream()
                .filter(type -> type.qualifiedName().equals("syntax.Java8Style"))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("name"), javaEight.fields().stream().map(JavaFieldFact::name).toList());
        assertEquals(List.of("name"), javaEight.methods().stream().map(JavaMethodFact::name).toList());
        assertEquals("Deprecated", javaEight.annotations().getFirst().name());
    }

    @Test
    void retainsModuleFileAndOneBasedSourceLocations() {
        Path fixture = FixturePaths.project("java-syntax-project");
        JavaFileDiscovery files = new JavaFileDiscoverer().discover(
                fixture,
                new JavaSourceRootDiscovery(
                        List.of(new JavaSourceRoot(
                                "pom.xml", ".", "src/main/java", SourceLocation.file("pom.xml"))),
                        List.of()));

        JavaTypeFact type = new JavaSourceParser().parse(fixture, files).compilationUnits().stream()
                .flatMap(unit -> unit.types().stream())
                .filter(candidate -> candidate.qualifiedName().equals("syntax.Java21Style"))
                .findFirst()
                .orElseThrow();

        assertEquals("src/main/java/syntax/Java21Style.java", type.location().sourceFileId());
        assertEquals(3, type.location().startLine().orElseThrow());
        assertTrue(type.location().startColumn().orElseThrow() >= 1);
        assertEquals("syntax.Java21Style", type.location().symbol().orElseThrow());
    }

    @Test
    void brokenFileProducesPartialFactsWithoutStoppingOtherFiles() throws IOException {
        write("src/main/java/demo/Healthy.java", "package demo; class Healthy {}\n");
        write("src/main/java/demo/Broken.java", "package demo; class Broken { void missing( }\n");
        JavaFileDiscovery files = new JavaFileDiscoverer().discover(
                scanRoot,
                new JavaSourceRootDiscovery(
                        List.of(new JavaSourceRoot(
                                "pom.xml", ".", "src/main/java", SourceLocation.file("pom.xml"))),
                        List.of()));

        JavaParseAnalysis analysis = new JavaSourceParser().parse(scanRoot, files);

        assertEquals(AnalysisStatus.PARTIAL, analysis.status());
        assertTrue(analysis.compilationUnits().stream()
                .flatMap(unit -> unit.types().stream())
                .anyMatch(type -> type.qualifiedName().equals("demo.Healthy")));
        assertTrue(analysis.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("JAVA_PARSE_PROBLEM")
                        && diagnostic.fileId().orElseThrow().endsWith("Broken.java")));
        assertFalse(analysis.compilationUnits().isEmpty());
    }

    private void write(String relativePath, String contents) throws IOException {
        Path file = scanRoot.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
    }
}
