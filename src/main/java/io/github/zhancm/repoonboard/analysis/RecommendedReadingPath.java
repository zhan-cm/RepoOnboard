package io.github.zhancm.repoonboard.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Complete reading path plus its bounded default presentation. */
public record RecommendedReadingPath(List<RecommendedReadingItem> items, int defaultLimit) {
    public static final int DEFAULT_LIMIT = 10;

    public RecommendedReadingPath {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (defaultLimit < 1 || defaultLimit > DEFAULT_LIMIT) {
            throw new IllegalArgumentException("defaultLimit must be between 1 and " + DEFAULT_LIMIT);
        }
        HashSet<String> sourceFileIds = new HashSet<>();
        for (RecommendedReadingItem item : items) {
            if (!sourceFileIds.add(item.sourceFileId())) {
                throw new IllegalArgumentException("items must be unique by sourceFileId");
            }
        }
    }

    /** The bounded list shown before a caller requests expansion. */
    public List<RecommendedReadingItem> defaultItems() {
        return List.copyOf(items.subList(0, Math.min(defaultLimit, items.size())));
    }

    /** Returns the default list or the retained complete list for an expanded view. */
    public List<RecommendedReadingItem> visibleItems(boolean expanded) {
        return expanded ? items : defaultItems();
    }

    public boolean expandable() {
        return items.size() > defaultLimit;
    }

    public int hiddenItemCount() {
        return Math.max(0, items.size() - defaultLimit);
    }
}
