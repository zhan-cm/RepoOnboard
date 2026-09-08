package io.github.zhancm.repoonboard.core.model;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A traceable location in a file under the scan root.
 *
 * <p>{@code sourceFileId} is the normalized, scan-root-relative file path. It
 * uses {@code /} separators and never contains an absolute path, an empty path
 * segment, {@code .}, or {@code ..}. Known lines and columns are 1-based and
 * inclusive. Unknown positions and symbols are represented by empty optionals,
 * never by zero or invented values.</p>
 */
public record SourceLocation(
        String sourceFileId,
        OptionalInt startLine,
        OptionalInt startColumn,
        OptionalInt endLine,
        OptionalInt endColumn,
        Optional<String> symbol) {

    public SourceLocation {
        sourceFileId = requireSourceFileId(sourceFileId);
        startLine = requirePosition(startLine, "startLine");
        startColumn = requirePosition(startColumn, "startColumn");
        endLine = requirePosition(endLine, "endLine");
        endColumn = requirePosition(endColumn, "endColumn");
        symbol = requireOptionalText(symbol, "symbol");

        if (startColumn.isPresent() && startLine.isEmpty()) {
            throw new IllegalArgumentException("startColumn requires startLine");
        }
        if (endLine.isPresent() && startLine.isEmpty()) {
            throw new IllegalArgumentException("endLine requires startLine");
        }
        if (endColumn.isPresent() && endLine.isEmpty()) {
            throw new IllegalArgumentException("endColumn requires endLine");
        }
        if (startLine.isPresent() && endLine.isPresent()
                && endLine.getAsInt() < startLine.getAsInt()) {
            throw new IllegalArgumentException("endLine must not precede startLine");
        }
        if (startLine.isPresent() && endLine.isPresent()
                && startLine.getAsInt() == endLine.getAsInt()
                && startColumn.isPresent() && endColumn.isPresent()
                && endColumn.getAsInt() < startColumn.getAsInt()) {
            throw new IllegalArgumentException("endColumn must not precede startColumn");
        }
    }

    /** Creates a file-only location when an exact source position is unknown. */
    public static SourceLocation file(String sourceFileId) {
        return new SourceLocation(
                sourceFileId,
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                Optional.empty());
    }

    static String requireSourceFileId(String sourceFileId) {
        Objects.requireNonNull(sourceFileId, "sourceFileId");
        if (sourceFileId.isBlank()) {
            throw new IllegalArgumentException("sourceFileId must not be blank");
        }
        if (sourceFileId.startsWith("/")
                || sourceFileId.contains("\\")
                || sourceFileId.contains(":")
                || sourceFileId.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "sourceFileId must be a normalized scan-root-relative path");
        }
        for (String segment : sourceFileId.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(
                        "sourceFileId must be a normalized scan-root-relative path");
            }
        }
        return sourceFileId;
    }

    private static OptionalInt requirePosition(OptionalInt position, String name) {
        Objects.requireNonNull(position, name);
        if (position.isPresent() && position.getAsInt() < 1) {
            throw new IllegalArgumentException(name + " must be 1-based when present");
        }
        return position;
    }

    private static Optional<String> requireOptionalText(Optional<String> value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isPresent() && value.orElseThrow().isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank when present");
        }
        return value;
    }
}
