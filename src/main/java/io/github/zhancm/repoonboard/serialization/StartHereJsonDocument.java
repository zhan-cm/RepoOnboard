package io.github.zhancm.repoonboard.serialization;

import io.github.zhancm.repoonboard.analysis.ExplainedReadingPath;
import io.github.zhancm.repoonboard.analysis.RecommendationExplanation;
import io.github.zhancm.repoonboard.analysis.RecommendationReason;
import io.github.zhancm.repoonboard.analysis.RecommendedReadingItem;
import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Explicit transport projection for the local Start Here UI. */
public record StartHereJsonDocument(
        String schemaVersion,
        String reportSchemaVersion,
        String projectId,
        String analysisStatus,
        boolean coverageLimited,
        List<String> coverageLimitationCodes,
        String coverageNotice,
        int defaultLimit,
        int totalItemCount,
        boolean expandable,
        int hiddenItemCount,
        List<ItemDto> items) {

    static StartHereJsonDocument fromModel(
            AnalysisReport report,
            ExplainedReadingPath explainedPath) {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(explainedPath, "explainedPath");
        List<RecommendedReadingItem> pathItems = explainedPath.path().items();
        List<RecommendationExplanation> explanations = explainedPath.explanations();
        List<ItemDto> items = new ArrayList<>(pathItems.size());
        for (int index = 0; index < pathItems.size(); index++) {
            items.add(ItemDto.fromModel(pathItems.get(index), explanations.get(index)));
        }
        return new StartHereJsonDocument(
                StartHereJson.currentSchemaVersion(),
                AnalysisReportJson.currentSchemaVersion(),
                report.project().id(),
                explainedPath.analysisStatus().name(),
                explainedPath.coverageLimited(),
                explainedPath.coverageLimitationCodes(),
                explainedPath.coverageNotice().orElse(null),
                explainedPath.path().defaultLimit(),
                items.size(),
                explainedPath.path().expandable(),
                explainedPath.path().hiddenItemCount(),
                List.copyOf(items));
    }

    public record ItemDto(
            String sourceFileId,
            String moduleId,
            List<ReasonDto> reasons) {

        static ItemDto fromModel(
                RecommendedReadingItem item,
                RecommendationExplanation explanation) {
            if (!item.sourceFileId().equals(explanation.sourceFileId())) {
                throw new IllegalArgumentException(
                        "Recommendation explanation must match its reading path item");
            }
            return new ItemDto(
                    item.sourceFileId(),
                    item.moduleId().orElse(null),
                    explanation.reasons().stream().map(ReasonDto::fromModel).toList());
        }
    }

    public record ReasonDto(
            String kind,
            String message,
            int factCount,
            Integer dependencyDistance,
            List<String> supportingEntityIds,
            List<ReportJsonDocument.EvidenceDto> evidence) {

        static ReasonDto fromModel(RecommendationReason reason) {
            return new ReasonDto(
                    reason.kind().name(),
                    reason.message(),
                    reason.factCount(),
                    reason.dependencyDistance().orElse(null),
                    reason.supportingEntityIds(),
                    reason.evidence().stream()
                            .map(ReportJsonDocument.EvidenceDto::fromModel)
                            .toList());
        }
    }
}
