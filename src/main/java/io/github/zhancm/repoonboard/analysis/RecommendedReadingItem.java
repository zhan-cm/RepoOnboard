package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.core.model.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** One file in the recommended reading path with all verified importance signals retained. */
public record RecommendedReadingItem(
        String sourceFileId,
        Optional<String> moduleId,
        List<ReadingImportanceAssessment> assessments) {

    public RecommendedReadingItem {
        sourceFileId = SourceLocation.file(sourceFileId).sourceFileId();
        moduleId = requireOptionalText(moduleId, "moduleId");
        assessments = List.copyOf(Objects.requireNonNull(assessments, "assessments"));
        if (assessments.isEmpty()) {
            throw new IllegalArgumentException("assessments must not be empty");
        }
        String expectedSourceFileId = sourceFileId;
        if (assessments.stream()
                .anyMatch(assessment -> !assessment.sourceFileId().equals(expectedSourceFileId))) {
            throw new IllegalArgumentException("All assessments must belong to sourceFileId");
        }
        Optional<String> expectedModuleId = moduleId;
        if (assessments.stream()
                .anyMatch(assessment -> !assessment.moduleId().equals(expectedModuleId))) {
            throw new IllegalArgumentException("All assessments must belong to moduleId");
        }
    }

    /** The highest-priority verified signal for this file. */
    public ReadingImportanceAssessment primaryAssessment() {
        return assessments.getFirst();
    }

    private static Optional<String> requireOptionalText(Optional<String> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(item -> {
            if (item.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
        });
        return value;
    }
}
