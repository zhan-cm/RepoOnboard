package io.github.zhancm.repoonboard.core.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Stable structural identifiers used by reports and UI references. */
public final class StableIdentifiers {
    private StableIdentifiers() {}

    public static String project(String rootPomFileId) {
        return "project:" + encode(SourceLocation.file(rootPomFileId).sourceFileId());
    }

    public static String module(String pomFileId) {
        return "module:" + encode(SourceLocation.file(pomFileId).sourceFileId());
    }

    public static String sourceFile(String path) {
        return "source:" + encode(SourceLocation.file(path).sourceFileId());
    }

    public static String component(String moduleId, String qualifiedName) {
        return "component:" + encode(requireText(moduleId, "moduleId")) + ":"
                + encode(requireText(qualifiedName, "qualifiedName"));
    }

    public static String entryPoint(String moduleId, String qualifiedName, String kind) {
        return "entry-point:" + digest(List.of(
                requireText(moduleId, "moduleId"),
                requireText(qualifiedName, "qualifiedName"),
                requireText(kind, "kind")));
    }

    public static String endpoint(
            String componentId,
            String handlerMethod,
            String httpMethod,
            Optional<String> path,
            boolean unresolvedPath,
            EndpointConditions conditions) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(conditions, "conditions");
        if (unresolvedPath == path.isPresent()) {
            throw new IllegalArgumentException("Exactly one of path or unresolvedPath must be set");
        }
        return "endpoint:" + digest(List.of(
                requireText(componentId, "componentId"),
                requireText(handlerMethod, "handlerMethod"),
                requireText(httpMethod, "httpMethod"),
                path.orElse(unresolvedPath ? "<unresolved>" : "<missing>"),
                Boolean.toString(unresolvedPath),
                canonical(conditions.params()),
                canonical(conditions.headers()),
                canonical(conditions.consumes()),
                canonical(conditions.produces()),
                Boolean.toString(conditions.unresolved())));
    }

    public static String dependency(
            DependencyKind kind,
            String sourceId,
            Optional<String> targetId,
            String declaredTarget) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(targetId, "targetId");
        return "dependency:" + digest(List.of(
                kind.name(), requireText(sourceId, "sourceId"),
                targetId.map(value -> requireText(value, "targetId")).orElse("<unresolved>"),
                requireText(declaredTarget, "declaredTarget")));
    }

    public static String externalArtifact(String coordinates) {
        return "artifact:" + encode(requireText(coordinates, "coordinates"));
    }

    private static String canonical(List<String> values) {
        return values.stream().sorted().map(StableIdentifiers::encode)
                .reduce((left, right) -> left + "," + right).orElse("");
    }

    private static String digest(List<String> parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                byte[] bytes = Objects.requireNonNull(part, "identity part")
                        .getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest(), 0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static String encode(String value) {
        StringBuilder result = new StringBuilder();
        for (byte current : value.getBytes(StandardCharsets.UTF_8)) {
            int unsigned = Byte.toUnsignedInt(current);
            if ((unsigned >= 'a' && unsigned <= 'z')
                    || (unsigned >= 'A' && unsigned <= 'Z')
                    || (unsigned >= '0' && unsigned <= '9')
                    || unsigned == '.' || unsigned == '_' || unsigned == '-') {
                result.append((char) unsigned);
            } else {
                result.append('%').append(HexFormat.of().withUpperCase().toHexDigits((byte) unsigned));
            }
        }
        return result.toString();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
