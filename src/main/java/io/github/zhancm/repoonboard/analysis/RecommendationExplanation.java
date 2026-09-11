package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;

/** All verified reasons attached to one file in the recommended reading path. */
public record RecommendationExplanation(
        String sourceFileId,
        List<RecommendationReason> reasons) {

    public RecommendationExplanation {
        sourceFileId = SourceLocation.file(sourceFileId).sourceFileId();
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("reasons must not be empty");
        }
    }
}
