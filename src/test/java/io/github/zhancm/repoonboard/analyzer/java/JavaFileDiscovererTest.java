package io.github.zhancm.repoonboard.analyzer.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaFileDiscovererTest {

    @TempDir
    Path scanRoot;

    @Test
    void enumeratesOnlyJavaFilesInStableOrder() throws IOException {
        JavaSourceRoot root = sourceRoot("pom.xml", ".", "src/main/java");
        write("src/main/java/example/Zed.java", "class Zed {}");
        write("src/main/java/example/Alpha.java", "class Alpha {}");
        write("src/main/java/example/readme.txt", "ignore");
        write("src/test/java/example/Test.java", "class Test {}");

        JavaFileDiscovery discovery = discover(List.of(root));

        assertEquals(AnalysisStatus.SUCCESS, discovery.status());
        assertEquals(
                List.of("src/main/java/example/Alpha.java", "src/main/java/example/Zed.java"),
                discovery.files().stream().map(JavaSourceFile::relativePath).toList());
        assertTrue(discovery.files().stream()
                .allMatch(file -> file.location().sourceFileId().equals(file.relativePath())));
    }

    @Test
    void preservesModuleOwnershipAcrossSourceRoots() throws IOException {
        JavaSourceRoot api = sourceRoot("api/pom.xml", "api", "api/src/main/java");
        JavaSourceRoot library = sourceRoot(
                "library/pom.xml", "library", "library/code/java");
        write("api/src/main/java/demo/Api.java", "class Api {}");
        write("library/code/java/demo/Library.java", "class Library {}");

        JavaFileDiscovery discovery = discover(List.of(api, library));

        assertEquals(2, discovery.files().size());
        assertEquals(
                List.of("api/pom.xml", "library/pom.xml"),
                discovery.files().stream().map(JavaSourceFile::modulePomFileId).toList());
        assertEquals(
                List.of("api", "library"),
                discovery.files().stream().map(JavaSourceFile::moduleBaseDirectory).toList());
    }

    @Test
    void skipsBuildAndGeneratedDirectoriesNestedUnderBroadSourceRoot() throws IOException {
        JavaSourceRoot broad = sourceRoot("pom.xml", ".", "src");
        write("src/main/java/demo/Main.java", "class Main {}");
        write("src/target/generated-sources/demo/Generated.java", "class Generated {}");
        write("src/generated-sources/demo/GeneratedAgain.java", "class GeneratedAgain {}");

        JavaFileDiscovery discovery = discover(List.of(broad));

        assertEquals(List.of("src/main/java/demo/Main.java"),
                discovery.files().stream().map(JavaSourceFile::relativePath).toList());
    }

    @Test
    void unavailableRootProducesPartialResultAndOtherRootsContinue() throws IOException {
        JavaSourceRoot missing = sourceRoot("missing/pom.xml", "missing", "missing/src/main/java");
        JavaSourceRoot available = sourceRoot("app/pom.xml", "app", "app/src/main/java");
        write("app/src/main/java/demo/App.java", "class App {}");

        JavaFileDiscovery discovery = discover(List.of(missing, available));

        assertEquals(AnalysisStatus.PARTIAL, discovery.status());
        assertEquals(1, discovery.files().size());
        assertTrue(discovery.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("JAVA_SOURCE_ROOT_UNAVAILABLE")));
    }

    @Test
    void overlappingModulesDoNotGuessFileOwnership() throws IOException {
        JavaSourceRoot first = sourceRoot("first/pom.xml", "first", "shared");
        JavaSourceRoot second = sourceRoot("second/pom.xml", "second", "shared");
        write("shared/example/Shared.java", "class Shared {}");

        JavaFileDiscovery discovery = discover(List.of(first, second));

        assertEquals(AnalysisStatus.PARTIAL, discovery.status());
        assertTrue(discovery.files().isEmpty());
        assertTrue(discovery.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("JAVA_FILE_MODULE_OWNERSHIP_AMBIGUOUS")));
    }

    private JavaFileDiscovery discover(List<JavaSourceRoot> roots) {
        return new JavaFileDiscoverer().discover(
                scanRoot, new JavaSourceRootDiscovery(roots, List.of()));
    }

    private JavaSourceRoot sourceRoot(String pom, String module, String path) {
        return new JavaSourceRoot(pom, module, path, SourceLocation.file(pom));
    }

    private void write(String relativePath, String contents) throws IOException {
        Path file = scanRoot.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
    }
}
