package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Recommended reading path with source-backed reasons and coverage context. */
public record ExplainedReadingPath(
        RecommendedReadingPath path,
        List<RecommendationExplanation> explanations,
        AnalysisStatus analysisStatus,
        List<String> coverageLimitationCodes,
        Optional<String> coverageNotice) {

    public ExplainedReadingPath {
        path = Objects.requireNonNull(path, "path");
        explanations = List.copyOf(Objects.requireNonNull(explanations, "explanations"));
        analysisStatus = Objects.requireNonNull(analysisStatus, "analysisStatus");
        coverageLimitationCodes = Objects.requireNonNull(
                        coverageLimitationCodes, "coverageLimitationCodes")
                .stream()
                .map(ExplainedReadingPath::requireText)
                .distinct()
                .sorted()
                .toList();
        coverageNotice = Objects.requireNonNull(coverageNotice, "coverageNotice");
        coverageNotice.ifPresent(ExplainedReadingPath::requireText);
        List<String> pathFiles = path.items().stream()
                .map(RecommendedReadingItem::sourceFileId)
                .toList();
        List<String> explanationFiles = explanations.stream()
                .map(RecommendationExplanation::sourceFileId)
                .toList();
        if (!pathFiles.equals(explanationFiles)) {
            throw new IllegalArgumentException(
                    "explanations must match the reading path files and order");
        }
        if (analysisStatus != AnalysisStatus.SUCCESS && coverageNotice.isEmpty()) {
            throw new IllegalArgumentException("Incomplete analysis requires a coverage notice");
        }
    }

    public List<RecommendationExplanation> defaultExplanations() {
        return List.copyOf(explanations.subList(0, path.defaultItems().size()));
    }

    public List<RecommendationExplanation> visibleExplanations(boolean expanded) {
        return expanded ? explanations : defaultExplanations();
    }

    public boolean coverageLimited() {
        return analysisStatus != AnalysisStatus.SUCCESS || !coverageLimitationCodes.isEmpty();
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        return value;
    }
}
