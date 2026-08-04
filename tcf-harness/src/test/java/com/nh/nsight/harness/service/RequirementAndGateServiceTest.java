package com.nh.nsight.harness.service;

import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.StageStatus;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.storage.JsonStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequirementAndGateServiceTest {
    @TempDir
    Path tempDir;

    private JsonStateRepository repository;
    private RequirementService requirements;
    private GateService gates;

    @BeforeEach
    void setUp() throws Exception {
        repository = new JsonStateRepository(tempDir);
        WorkItemState state = WorkItemState.create(
                "REQ-20260802-001", "고객 조회", tempDir.toString(), "harness/REQ-20260802-001-customer");
        repository.save(state);
        requirements = new RequirementService(repository, tempDir);
        gates = new GateService(repository);
    }

    @Test
    void exposesExactlyTwelveFixedRequirementQuestions() {
        assertThat(requirements.questions()).hasSize(12);
        assertThat(requirements.questions().getFirst().id()).isEqualTo("REQ-Q01");
        assertThat(requirements.questions().getLast().id()).isEqualTo("REQ-Q12");
    }

    @Test
    void blocksAnalysisUntilRequirementIsApproved() {
        assertThatThrownBy(() -> gates.startStage("REQ-20260802-001", Stage.ANALYSIS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires approved REQUIREMENT");
    }

    @Test
    void allowsAnalysisAfterManualRequirementApproval() throws Exception {
        gates.review("REQ-20260802-001", Stage.REQUIREMENT);
        gates.decide("REQ-20260802-001", Stage.REQUIREMENT, StageStatus.APPROVED, "approved");

        gates.startStage("REQ-20260802-001", Stage.ANALYSIS);

        assertThat(repository.load("REQ-20260802-001").stage(Stage.ANALYSIS).status())
                .isEqualTo(StageStatus.IN_PROGRESS);
    }
}
