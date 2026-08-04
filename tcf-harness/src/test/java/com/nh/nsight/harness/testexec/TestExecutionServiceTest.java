package com.nh.nsight.harness.testexec;

import com.nh.nsight.harness.domain.TestCommand;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.git.GitService;
import com.nh.nsight.harness.storage.JsonStateRepository;
import com.nh.nsight.harness.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TestExecutionServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void detectsGradleTestCandidates() throws Exception {
        Files.createFile(tempDir.resolve("gradlew"));

        assertThat(new TestCommandDetector().detect(tempDir))
                .extracting(TestCommand::id)
                .containsExactly("UNIT_TEST", "QUALITY_GATE");
    }

    @Test
    void stopsAfterThreeFailedAttempts() throws Exception {
        JsonStateRepository repository = new JsonStateRepository(tempDir);
        WorkItemState state = WorkItemState.create(
                "REQ-20260802-001", "테스트", tempDir.toString(), "harness/REQ-20260802-001-test");
        state.addTestCommand(new TestCommand("FAIL", "exit 1", true, 10));
        repository.save(state);
        ProcessRunner runner = new ProcessRunner();
        GitService git = new GitService(tempDir, runner);
        TestExecutionService service = new TestExecutionService(
                tempDir, repository, runner, new TestEvidenceWriter(tempDir, git));

        TestRunSummary result = service.execute("REQ-20260802-001", 3, (attempt, partial) -> true);

        assertThat(result.success()).isFalse();
        assertThat(result.attempts()).isEqualTo(3);
        assertThat(result.verdict()).isEqualTo("NEEDS_HUMAN_REVIEW");
        assertThat(result.results()).hasSize(3);
    }
}
