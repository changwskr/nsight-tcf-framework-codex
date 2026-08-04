package com.nh.nsight.harness.service;

import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.StageState;
import com.nh.nsight.harness.domain.StageStatus;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.storage.JsonStateRepository;

import java.io.IOException;
import java.util.Set;

public final class GateService {
    private static final Set<StageStatus> DECISIONS = Set.of(
            StageStatus.APPROVED,
            StageStatus.REVISION_REQUIRED,
            StageStatus.REJECTED
    );

    private final JsonStateRepository repository;

    public GateService(JsonStateRepository repository) {
        this.repository = repository;
    }

    public WorkItemState startStage(String workItemId, Stage stage) throws IOException {
        WorkItemState state = repository.load(workItemId);
        stage.prerequisite().ifPresent(prerequisite -> {
            if (state.stage(prerequisite).status() != StageStatus.APPROVED) {
                throw new IllegalStateException(stage + " requires approved " + prerequisite);
            }
        });
        StageState current = state.stage(stage);
        Set<StageStatus> startable = Set.of(
                StageStatus.NOT_STARTED,
                StageStatus.IN_PROGRESS,
                StageStatus.REVISION_REQUIRED,
                StageStatus.FAILED,
                StageStatus.NEEDS_HUMAN_REVIEW
        );
        if (!startable.contains(current.status())) {
            throw new IllegalStateException("Stage cannot start from " + current.status() + ": " + stage);
        }
        state.updateStage(stage, current.transition(StageStatus.IN_PROGRESS, "system", "stage started"));
        repository.save(state);
        return state;
    }

    public WorkItemState review(String workItemId, Stage stage) throws IOException {
        WorkItemState state = repository.load(workItemId);
        StageState current = state.stage(stage);
        if (current.status() != StageStatus.IN_PROGRESS && current.status() != StageStatus.REVISION_REQUIRED
                && current.status() != StageStatus.NEEDS_HUMAN_REVIEW) {
            throw new IllegalStateException("Stage cannot move to REVIEW from " + current.status());
        }
        state.updateStage(stage, current.transition(StageStatus.REVIEW, "system", "artifact ready for review"));
        repository.save(state);
        return state;
    }

    public WorkItemState decide(String workItemId, Stage stage, StageStatus decision, String comment) throws IOException {
        if (!DECISIONS.contains(decision)) {
            throw new IllegalArgumentException("Not a manual Gate decision: " + decision);
        }
        WorkItemState state = repository.load(workItemId);
        if (state.stage(stage).status() != StageStatus.REVIEW) {
            throw new IllegalStateException("Manual decision requires REVIEW status: " + stage);
        }
        state.updateStage(stage, state.stage(stage).transition(decision, "user", comment));
        repository.save(state);
        return state;
    }

    public WorkItemState markNeedsHumanReview(String workItemId, Stage stage, String comment) throws IOException {
        WorkItemState state = repository.load(workItemId);
        state.updateStage(stage, state.stage(stage).transition(StageStatus.NEEDS_HUMAN_REVIEW, "system", comment));
        repository.save(state);
        return state;
    }
}
