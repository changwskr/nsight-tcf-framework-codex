package com.nh.nsight.harness.service;

import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.WorkItemState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ArtifactValidator {
    private final Path repositoryRoot;

    public ArtifactValidator(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    public void validateForReview(WorkItemState state, Stage stage) throws IOException {
        if (stage == Stage.REQUIREMENT && state.requirementAnswers().size() != 12) {
            throw new IllegalStateException("REQUIREMENT review requires all 12 answers");
        }
        for (Path required : requiredArtifacts(state.workItemId(), stage)) {
            if (!Files.exists(required) || Files.size(required) == 0) {
                throw new IllegalStateException("Required stage artifact is missing or empty: " + required);
            }
        }
    }

    public List<Path> requiredArtifacts(String workItemId, Stage stage) {
        Path workItem = repositoryRoot.resolve("docs/work-items").resolve(workItemId);
        return switch (stage) {
            case REQUIREMENT -> List.of(workItem.resolve("requirement.md"));
            case ANALYSIS -> List.of(workItem.resolve("analysis.md"));
            case DESIGN -> List.of(workItem.resolve("design.md"), workItem.resolve("execution-plan.md"));
            case IMPLEMENTATION -> List.of(workItem.resolve("implementation-result.md"));
            case TEST -> List.of(
                    workItem.resolve("test-evidence/test-summary.md"),
                    workItem.resolve("test-evidence/environment.json"),
                    workItem.resolve("test-evidence/commands.json"),
                    workItem.resolve("test-evidence/git-diff.patch")
            );
            case CLOSE -> List.of(workItem.resolve("closure.md"));
        };
    }
}
