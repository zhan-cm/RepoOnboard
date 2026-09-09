package io.github.zhancm.repoonboard.analyzer.spring;

import java.util.Objects;

/** Module-aware identity of a Spring component fact. */
public record SpringComponentRef(String modulePomFileId, String qualifiedName)
        implements Comparable<SpringComponentRef> {

    public SpringComponentRef {
        modulePomFileId = requireText(modulePomFileId, "modulePomFileId");
        qualifiedName = requireText(qualifiedName, "qualifiedName");
    }

    @Override
    public int compareTo(SpringComponentRef other) {
        int module = modulePomFileId.compareTo(other.modulePomFileId);
        return module != 0 ? module : qualifiedName.compareTo(other.qualifiedName);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
