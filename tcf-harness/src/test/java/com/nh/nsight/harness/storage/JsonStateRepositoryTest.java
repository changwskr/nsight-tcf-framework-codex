package com.nh.nsight.harness.storage;

import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.WorkItemState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JsonStateRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsStateWithoutLosingStageData() throws Exception {
        JsonStateRepository repository = new JsonStateRepository(tempDir);
        WorkItemState state = WorkItemState.create(
                "REQ-20260802-001", "고객 조회", tempDir.toString(), "harness/REQ-20260802-001-customer");

        repository.save(state);
        WorkItemState loaded = repository.load(state.workItemId());

        assertThat(loaded.workItemId()).isEqualTo(state.workItemId());
        assertThat(loaded.title()).isEqualTo("고객 조회");
        assertThat(loaded.stage(Stage.REQUIREMENT).status()).isEqualTo(state.stage(Stage.REQUIREMENT).status());
        assertThat(repository.pathFor(state.workItemId())).exists();
    }
}
