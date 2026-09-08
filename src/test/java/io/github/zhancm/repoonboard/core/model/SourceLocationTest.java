package io.github.zhancm.repoonboard.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class SourceLocationTest {

    @Test
    void representsUnknownPositionWithoutInventedCoordinates() {
        SourceLocation location = SourceLocation.file("src/main/java/example/App.java");

        assertEquals("src/main/java/example/App.java", location.sourceFileId());
        assertFalse(location.startLine().isPresent());
        assertFalse(location.startColumn().isPresent());
        assertFalse(location.endLine().isPresent());
        assertFalse(location.endColumn().isPresent());
        assertFalse(location.symbol().isPresent());
    }

    @Test
    void acceptsOneBasedInclusiveRange() {
        SourceLocation location = new SourceLocation(
                "src/main/java/example/App.java",
                OptionalInt.of(4),
                OptionalInt.of(3),
                OptionalInt.of(6),
                OptionalInt.of(9),
                Optional.of("example.App"));

        assertEquals(4, location.startLine().orElseThrow());
        assertEquals(9, location.endColumn().orElseThrow());
        assertEquals("example.App", location.symbol().orElseThrow());
    }

    @Test
    void rejectsAbsoluteOrUnnormalizedPaths() {
        assertThrows(IllegalArgumentException.class, () -> SourceLocation.file("/src/App.java"));
        assertThrows(IllegalArgumentException.class, () -> SourceLocation.file("C:/src/App.java"));
        assertThrows(IllegalArgumentException.class, () -> SourceLocation.file("src\\App.java"));
        assertThrows(IllegalArgumentException.class, () -> SourceLocation.file("src/../App.java"));
        assertThrows(IllegalArgumentException.class, () -> SourceLocation.file("src//App.java"));
    }

    @Test
    void rejectsZeroAndIncompletePositions() {
        assertThrows(IllegalArgumentException.class, () -> location(
                OptionalInt.of(0), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty()));
        assertThrows(IllegalArgumentException.class, () -> location(
                OptionalInt.empty(), OptionalInt.of(1), OptionalInt.empty(), OptionalInt.empty()));
        assertThrows(IllegalArgumentException.class, () -> location(
                OptionalInt.of(1), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(1)));
    }

    @Test
    void rejectsBackwardsRanges() {
        assertThrows(IllegalArgumentException.class, () -> location(
                OptionalInt.of(5), OptionalInt.empty(), OptionalInt.of(4), OptionalInt.empty()));
        assertThrows(IllegalArgumentException.class, () -> location(
                OptionalInt.of(5), OptionalInt.of(8), OptionalInt.of(5), OptionalInt.of(7)));
    }

    private static SourceLocation location(
            OptionalInt startLine,
            OptionalInt startColumn,
            OptionalInt endLine,
            OptionalInt endColumn) {
        return new SourceLocation(
                "src/App.java",
                startLine,
                startColumn,
                endLine,
                endColumn,
                Optional.empty());
    }
}
