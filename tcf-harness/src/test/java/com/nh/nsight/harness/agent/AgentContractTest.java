package com.nh.nsight.harness.agent;

import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.prompt.PromptService;
import com.nh.nsight.harness.prompt.StagePromptContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AgentContractTest {
    @TempDir
    Path tempDir;

    @Test
    void createsFileBasedPromptContract() throws Exception {
        Path prompts = tempDir.resolve("harness/prompts");
        Files.createDirectories(prompts);
        Files.writeString(prompts.resolve("MASTER-HARNESS.md"), "master ${workItemId}", StandardCharsets.UTF_8);
        Files.writeString(prompts.resolve("02-ANALYSIS.md"), "analysis ${title}", StandardCharsets.UTF_8);
        WorkItemState state = WorkItemState.create(
                "REQ-20260802-001", "고객 조회", tempDir.toString(), "harness/REQ-20260802-001-customer");

        StagePromptContract contract = new PromptService(tempDir).createContract(state, Stage.ANALYSIS);

        assertThat(contract.request().promptFile()).exists();
        assertThat(contract.request().contextFile()).exists();
        assertThat(contract.request().resultFile()).exists();
        assertThat(Files.readString(contract.request().promptFile())).contains("REQ-20260802-001", "고객 조회");
    }
}
