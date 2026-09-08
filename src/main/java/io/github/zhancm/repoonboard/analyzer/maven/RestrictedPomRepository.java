package io.github.zhancm.repoonboard.analyzer.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

final class RestrictedPomRepository {

    enum Origin {
        SCAN_ROOT,
        LOCAL_REPOSITORY
    }

    private static final String ROOT_POM_ID = "pom.xml";
    private static final String LOCAL_REPOSITORY_PREFIX = "local-repository/";

    private final Path scanRoot;
    private final Path localRepository;
    private final long maximumPomBytes;
    private final Map<Path, RestrictedPomSource> sourcesByRealPath = new LinkedHashMap<>();
    private final Map<String, RestrictedPomSource> sourcesById = new LinkedHashMap<>();
    private final List<PomSourceProblem> problems = new ArrayList<>();

    RestrictedPomRepository(Path scanRoot, MavenModelOptions options) throws IOException {
        this.scanRoot = scanRoot.toRealPath();
        this.localRepository = realOrNormalized(options.localRepository());
        this.maximumPomBytes = options.maximumPomBytes();
    }

    RestrictedPomSource rootSource() throws RestrictedPomException {
        return source(scanRoot.resolve(ROOT_POM_ID), scanRoot, Origin.SCAN_ROOT);
    }

    Optional<RestrictedPomSource> localRepositorySource(
            String groupId, String artifactId, String version) {
        if (!validGroupId(groupId) || !validCoordinatePart(artifactId)
                || !validCoordinatePart(version)) {
            recordProblem(
                    "MAVEN_MODEL_COORDINATES_REJECTED",
                    ROOT_POM_ID,
                    "A parent or BOM coordinate cannot be mapped safely to the local repository.");
            return Optional.empty();
        }

        Path candidate = localRepository;
        for (String segment : groupId.split("\\.")) {
            candidate = candidate.resolve(segment);
        }
        candidate = candidate.resolve(artifactId)
                .resolve(version)
                .resolve(artifactId + "-" + version + ".pom");
        if (!Files.exists(candidate)) {
            return Optional.empty();
        }
        try {
            return Optional.of(source(candidate, localRepository, Origin.LOCAL_REPOSITORY));
        } catch (RestrictedPomException exception) {
            recordProblem(exception.code(), exception.sourceFileId(), exception.getMessage());
            return Optional.empty();
        }

    }

    Optional<RestrictedPomSource> relatedSource(
            RestrictedPomSource source, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return Optional.empty();
        }

        final Path candidatePath;
        try {
            candidatePath = source.realFile().getParent().resolve(relativePath).normalize();
        } catch (InvalidPathException exception) {
            recordProblem("MAVEN_POM_PATH_INVALID", source.sourceFileId(),
                    "A relative parent path is invalid on this platform.");
            return Optional.empty();
        }
        Path candidate = candidatePath;
        if (!candidate.startsWith(source.boundaryRoot())) {
            recordProblem("MAVEN_POM_SOURCE_OUTSIDE_ALLOWED_ROOT", source.sourceFileId(),
                    "A referenced POM resolves outside its allowed local root.");
            return Optional.empty();
        }
        if (Files.isDirectory(candidate)) {
            candidate = candidate.resolve(ROOT_POM_ID);
        }
        if (!Files.exists(candidate)) {
            return Optional.empty();
        }
        try {
            return Optional.of(source(
                    candidate, source.boundaryRoot(), source.origin()));
        } catch (RestrictedPomException exception) {
            recordProblem(exception.code(), source.sourceFileId(), exception.getMessage());
            return Optional.empty();
        }
    }

    Collection<RestrictedPomSource> sources() {
        return List.copyOf(sourcesById.values());
    }

    Optional<RestrictedPomSource> sourceById(String sourceFileId) {
        return Optional.ofNullable(sourcesById.get(sourceFileId));
    }

    List<PomSourceProblem> problems() {
        return List.copyOf(problems);
    }

    boolean recognizesSource(String sourceFileId) {
        return sourcesById.containsKey(sourceFileId);
    }

    private RestrictedPomSource source(
            Path candidate,
            Path boundaryRoot,
            Origin origin) throws RestrictedPomException {
        String provisionalId = origin == Origin.SCAN_ROOT
                ? safeScanId(candidate)
                : safeLocalRepositoryId(candidate);
        final Path realFile;
        try {
            realFile = candidate.toRealPath();
        } catch (IOException exception) {
            throw new RestrictedPomException(
                    "MAVEN_POM_SOURCE_UNAVAILABLE",
                    provisionalId,
                    "A referenced POM source is unavailable.");
        }
        if (!realFile.startsWith(boundaryRoot)) {
            throw new RestrictedPomException(
                    "MAVEN_POM_SOURCE_OUTSIDE_ALLOWED_ROOT",
                    provisionalId,
                    "A referenced POM resolves outside its allowed local root.");
        }
        if (!Files.isRegularFile(realFile)) {
            throw new RestrictedPomException(
                    "MAVEN_POM_SOURCE_NOT_REGULAR_FILE",
                    provisionalId,
                    "A referenced POM is not a regular file.");
        }

        RestrictedPomSource cached = sourcesByRealPath.get(realFile);
        if (cached != null) {
            return cached;
        }
        if (sourcesByRealPath.size() >= 64) {
            throw new RestrictedPomException("MAVEN_POM_SOURCE_LIMIT", provisionalId,
                    "The scan exceeds the limit of 64 distinct POM sources.");
        }

        String sourceFileId = origin == Origin.SCAN_ROOT
                ? relativeId(scanRoot, realFile)
                : LOCAL_REPOSITORY_PREFIX + relativeId(localRepository, realFile);
        byte[] snapshot = readBounded(realFile, sourceFileId);
        validateXml(snapshot, sourceFileId);
        Model rawModel = readRawModel(snapshot, sourceFileId);
        RestrictedPomSource created = new RestrictedPomSource(
                this,
                realFile,
                boundaryRoot,
                origin,
                sourceFileId,
                snapshot,
                rawModel);
        sourcesByRealPath.put(realFile, created);
        sourcesById.put(sourceFileId, created);
        return created;
    }

    private byte[] readBounded(Path file, String sourceFileId) throws RestrictedPomException {
        int limit = Math.toIntExact(Math.min(maximumPomBytes, Integer.MAX_VALUE - 1L));
        try (InputStream input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(limit + 1);
            if (bytes.length > limit) {
                throw new RestrictedPomException(
                        "MAVEN_POM_TOO_LARGE",
                        sourceFileId,
                        "A POM exceeds the configured input size limit.");
            }
            return bytes;
        } catch (RestrictedPomException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RestrictedPomException(
                    "MAVEN_POM_SOURCE_UNAVAILABLE",
                    sourceFileId,
                    "A referenced POM source could not be read.");
        }
    }

    private static void validateXml(byte[] snapshot, String sourceFileId)
            throws RestrictedPomException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        try {
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
            factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
                throw new XMLStreamException("External XML access is forbidden");
            });
        } catch (IllegalArgumentException exception) {
            throw new RestrictedPomException(
                    "MAVEN_XML_SECURITY_UNAVAILABLE",
                    sourceFileId,
                    "The runtime XML parser cannot enforce the required Maven security policy.");
        }

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(snapshot));
            try {
                int depth = 0;
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT && ++depth > 128) {
                        throw new RestrictedPomException("MAVEN_POM_XML_DEPTH_LIMIT", sourceFileId,
                                "A POM exceeds the maximum XML nesting depth of 128.");
                    }
                    if (event == XMLStreamConstants.END_ELEMENT) {
                        depth--;
                    }
                    if (event == XMLStreamConstants.DTD) {
                        throw new RestrictedPomException(
                                "MAVEN_POM_DTD_FORBIDDEN",
                                sourceFileId,
                                "DTD and external entity declarations are forbidden in Maven POM input.");
                    }
                }
            } finally {
                reader.close();
            }
        } catch (RestrictedPomException exception) {
            throw exception;
        } catch (XMLStreamException exception) {
            throw new RestrictedPomException(
                    "MAVEN_POM_XML_INVALID",
                    sourceFileId,
                    "A POM is not well-formed XML or attempted forbidden external access.");
        }
    }

    private static Model readRawModel(byte[] snapshot, String sourceFileId)
            throws RestrictedPomException {
        InputSource inputSource = new InputSource();
        inputSource.setLocation(sourceFileId);
        MavenXpp3ReaderEx reader = new MavenXpp3ReaderEx();
        reader.setAddDefaultEntities(false);
        try {
            return reader.read(new ByteArrayInputStream(snapshot), true, inputSource);
        } catch (IOException | XmlPullParserException exception) {
            throw new RestrictedPomException(
                    "MAVEN_POM_MODEL_INVALID",
                    sourceFileId,
                    "A POM could not be read as Maven model data.");
        }
    }

    private void recordProblem(String code, String sourceFileId, String message) {
        PomSourceProblem problem = new PomSourceProblem(code, sourceFileId, message);
        if (!problems.contains(problem)) {
            problems.add(problem);
        }
    }

    private String safeScanId(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        return normalized.startsWith(scanRoot) ? relativeId(scanRoot, normalized) : ROOT_POM_ID;
    }

    private String safeLocalRepositoryId(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        return normalized.startsWith(localRepository)
                ? LOCAL_REPOSITORY_PREFIX + relativeId(localRepository, normalized)
                : LOCAL_REPOSITORY_PREFIX + "rejected.pom";

    }

    private static Path realOrNormalized(Path path) throws IOException {
        return Files.exists(path) ? path.toRealPath() : path.toAbsolutePath().normalize();
    }

    private static String relativeId(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static boolean validGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return false;
        }
        for (String segment : groupId.split("\\.", -1)) {
            if (!validCoordinatePart(segment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validCoordinatePart(String value) {
        return value != null
                && !value.isBlank()
                && value.matches("[A-Za-z0-9_+.-]+")
                && !value.equals(".")
                && !value.equals("..")
                && !value.contains("..");
    }
}
