package io.github.zhancm.repoonboard.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiagnosticTest {

    @Test
    void retainsStructuredContext() {
        SourceLocation location = SourceLocation.file("module-a/pom.xml");

        Diagnostic diagnostic = new Diagnostic(
                "MAVEN_PARENT_UNRESOLVED",
                DiagnosticSeverity.WARNING,
                "MAVEN_MODEL",
                Optional.of("module-a"),
                Optional.of("module-a/pom.xml"),
                Optional.of(location),
                "Parent metadata could not be resolved; local analysis continued.");

        assertEquals(DiagnosticSeverity.WARNING, diagnostic.severity());
        assertEquals("module-a", diagnostic.moduleId().orElseThrow());
        assertEquals(location, diagnostic.location().orElseThrow());
    }

    @Test
    void rejectsMismatchedFileAndLocation() {
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(
                "SOURCE_PARSE_WARNING",
                DiagnosticSeverity.WARNING,
                "JAVA_PARSE",
                Optional.empty(),
                Optional.of("src/Other.java"),
                Optional.of(SourceLocation.file("src/App.java")),
                "One source file could not be parsed."));
    }

    @Test
    void permitsAProjectLevelDiagnosticWithoutFileContext() {
        Diagnostic diagnostic = new Diagnostic(
                "INVALID_SCAN_ROOT",
                DiagnosticSeverity.ERROR,
                "INPUT",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "The scan root is invalid.");

        assertEquals(Optional.empty(), diagnostic.fileId());
        assertEquals(Optional.empty(), diagnostic.location());
    }
}
