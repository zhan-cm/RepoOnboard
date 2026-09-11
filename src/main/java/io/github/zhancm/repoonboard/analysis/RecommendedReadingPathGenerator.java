package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/** Builds a stable file-level reading path from deterministic importance signals. */
public final class RecommendedReadingPathGenerator {
    private final ReadingImportanceHeuristics heuristics;

    public RecommendedReadingPathGenerator() {
        this(new ReadingImportanceHeuristics());
    }

    RecommendedReadingPathGenerator(ReadingImportanceHeuristics heuristics) {
        this.heuristics = Objects.requireNonNull(heuristics, "heuristics");
    }

    public RecommendedReadingPath generate(AnalysisReport report) {
        Objects.requireNonNull(report, "report");
        List<ReadingImportanceAssessment> assessments = heuristics.evaluate(report);
        Map<String, List<ReadingImportanceAssessment>> assessmentsByFile = new LinkedHashMap<>();
        for (ReadingImportanceAssessment assessment : assessments) {
            assessmentsByFile.computeIfAbsent(
                    assessment.sourceFileId(), ignored -> new ArrayList<>()).add(assessment);
        }

        List<RecommendedReadingItem> items = assessmentsByFile.entrySet().stream()
                .map(entry -> new RecommendedReadingItem(
                        entry.getKey(), moduleId(entry.getValue()), entry.getValue()))
                .toList();
        return new RecommendedReadingPath(items, RecommendedReadingPath.DEFAULT_LIMIT);
    }

    private static Optional<String> moduleId(List<ReadingImportanceAssessment> assessments) {
        TreeSet<String> moduleIds = new TreeSet<>();
        assessments.forEach(assessment -> assessment.moduleId().ifPresent(moduleIds::add));
        if (moduleIds.size() > 1) {
            throw new IllegalArgumentException("A source file cannot belong to multiple modules");
        }
        return moduleIds.stream().findFirst();
    }
}
