package io.github.zhancm.repoonboard.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvidenceTest {

    @Test
    void defensivelyCopiesRelatedLocations() {
        SourceLocation declaration = SourceLocation.file("src/App.java");
        List<SourceLocation> related = new ArrayList<>();
        related.add(SourceLocation.file("pom.xml"));

        Evidence evidence = new Evidence(
                "DECLARATION", declaration, related, "java.type-declaration");
        related.clear();

        assertEquals(1, evidence.relatedLocations().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> evidence.relatedLocations().add(declaration));
    }

    @Test
    void requiresMachineReadableIdentifiersAndLocation() {
        SourceLocation location = SourceLocation.file("pom.xml");

        assertThrows(IllegalArgumentException.class,
                () -> new Evidence(" ", location, List.of(), "maven.project"));
        assertThrows(NullPointerException.class,
                () -> new Evidence("BUILD_METADATA", null, List.of(), "maven.project"));
        assertThrows(IllegalArgumentException.class,
                () -> new Evidence("BUILD_METADATA", location, List.of(), " "));
    }
}
