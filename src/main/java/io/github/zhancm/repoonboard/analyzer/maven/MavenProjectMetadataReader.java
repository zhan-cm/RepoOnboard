package io.github.zhancm.repoonboard.analyzer.maven;

import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/** Reads only the basic, raw metadata needed by T-0202. */
public final class MavenProjectMetadataReader {

    private static final String POM_FILE_NAME = "pom.xml";
    private static final String STAGE = "MAVEN_METADATA";
    private static final String MAVEN_PROPERTY_PREFIX = "${";

    public MavenProjectMetadata read(Path scanRoot) {
        if (scanRoot == null || !Files.isDirectory(scanRoot)) {
            throw new IllegalArgumentException("scanRoot must be an existing directory");
        }

        Path pom = scanRoot.resolve(POM_FILE_NAME);
        if (!Files.isRegularFile(pom)) {
            throw new IllegalArgumentException("scanRoot must contain a root pom.xml file");
        }

        try {
            Model model = readRawModel(pom);
            List<Diagnostic> diagnostics = new ArrayList<>();
            MavenMetadataValue groupId = value(model, "groupId", Model::getGroupId, null, diagnostics);
            MavenMetadataValue artifactId = value(
                    model, "artifactId", Model::getArtifactId, null, diagnostics);
            MavenMetadataValue version = value(model, "version", Model::getVersion, null, diagnostics);
            MavenMetadataValue packaging = value(
                    model, "packaging", Model::getPackaging, "jar", diagnostics);
            return new MavenProjectMetadata(
                    groupId, artifactId, version, packaging, diagnostics);
        } catch (IOException | XmlPullParserException exception) {
            SourceLocation pomLocation = SourceLocation.file(POM_FILE_NAME);
            Diagnostic diagnostic = new Diagnostic(
                    "MAVEN_POM_READ_FAILED",
                    DiagnosticSeverity.ERROR,
                    STAGE,
                    Optional.empty(),
                    Optional.of(POM_FILE_NAME),
                    Optional.of(pomLocation),
                    "The root pom.xml could not be read as Maven metadata.");
            MavenMetadataValue unavailable = missingValue(pomLocation);
            return new MavenProjectMetadata(
                    unavailable, unavailable, unavailable, unavailable, List.of(diagnostic));
        }
    }

    private static Model readRawModel(Path pom) throws IOException, XmlPullParserException {
        InputSource source = new InputSource();
        source.setLocation(POM_FILE_NAME);
        MavenXpp3ReaderEx modelReader = new MavenXpp3ReaderEx();
        modelReader.setAddDefaultEntities(false);
        try (InputStream input = Files.newInputStream(pom)) {
            return modelReader.read(input, true, source);
        }
    }

    private static MavenMetadataValue value(
            Model model,
            String field,
            Function<Model, String> getter,
            String defaultValue,
            List<Diagnostic> diagnostics) {
        InputLocation inputLocation = model.getLocation(field);
        SourceLocation origin = sourceLocation(field, inputLocation);
        Optional<String> rawValue = inputLocation == null
                ? Optional.empty()
                : optionalText(getter.apply(model));

        if (rawValue.isPresent()) {
            String raw = rawValue.orElseThrow();
            if (raw.contains(MAVEN_PROPERTY_PREFIX)) {
                diagnostics.add(diagnostic(
                        "MAVEN_METADATA_UNRESOLVED",
                        origin,
                        "Maven metadata field requires property resolution: " + field));
                return new MavenMetadataValue(
                        rawValue,
                        Optional.empty(),
                        MavenResolutionStatus.UNRESOLVED,
                        origin);
            }
            return new MavenMetadataValue(
                    rawValue,
                    rawValue,
                    MavenResolutionStatus.RESOLVED,
                    origin);
        }

        if (defaultValue != null) {
            return new MavenMetadataValue(
                    Optional.empty(),
                    Optional.of(defaultValue),
                    MavenResolutionStatus.RESOLVED,
                    origin);
        }

        diagnostics.add(diagnostic(
                "MAVEN_METADATA_MISSING",
                origin,
                "Maven metadata field is missing: " + field));
        return missingValue(origin);
    }

    private static MavenMetadataValue missingValue(SourceLocation origin) {
        return new MavenMetadataValue(
                Optional.empty(),
                Optional.empty(),
                MavenResolutionStatus.MISSING,
                origin);
    }

    private static Diagnostic diagnostic(
            String code, SourceLocation location, String message) {
        return new Diagnostic(
                code,
                DiagnosticSeverity.WARNING,
                STAGE,
                Optional.empty(),
                Optional.of(POM_FILE_NAME),
                Optional.of(location),
                message);
    }

    private static SourceLocation sourceLocation(String field, InputLocation inputLocation) {
        OptionalInt line = positive(inputLocation == null ? 0 : inputLocation.getLineNumber());
        OptionalInt column = positive(inputLocation == null ? 0 : inputLocation.getColumnNumber());
        return new SourceLocation(
                POM_FILE_NAME,
                line,
                column,
                OptionalInt.empty(),
                OptionalInt.empty(),
                Optional.of("project." + field));
    }

    private static OptionalInt positive(int value) {
        return value > 0 ? OptionalInt.of(value) : OptionalInt.empty();
    }

    private static Optional<String> optionalText(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
