package com.nh.nsight.harness;

import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.StageStatus;
import com.nh.nsight.harness.domain.TestCommand;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.git.GitService;
import com.nh.nsight.harness.prompt.PromptService;
import com.nh.nsight.harness.service.GateService;
import com.nh.nsight.harness.service.RequirementService;
import com.nh.nsight.harness.storage.JsonStateRepository;
import com.nh.nsight.harness.testexec.TestCommandDetector;
import com.nh.nsight.harness.testexec.TestEvidenceWriter;
import com.nh.nsight.harness.testexec.TestExecutionService;
import com.nh.nsight.harness.testexec.TestRunSummary;
import com.nh.nsight.harness.util.ProcessRunner;
import com.nh.nsight.harness.util.SensitiveDataGuard;

import java.nio.file.Files;
import java.nio.file.Path;

public final class OfflineHarnessSmokeTest {
    private OfflineHarnessSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("harness-smoke-");
        JsonStateRepository repository = new JsonStateRepository(root);
        WorkItemState state = WorkItemState.create(
                "REQ-20260802-001",
                "customer-inquiry",
                root.toString(),
                "harness/REQ-20260802-001-customer-inquiry"
        );
        repository.save(state);
        WorkItemState loaded = repository.load(state.workItemId());
        check(loaded.workItemId().equals(state.workItemId()), "state round trip");

        RequirementService requirements = new RequirementService(repository, root);
        check(requirements.questions().size() == 12, "twelve requirement questions");

        GateService gates = new GateService(repository);
        boolean blocked = false;
        try {
            gates.startStage(state.workItemId(), Stage.ANALYSIS);
        } catch (IllegalStateException expected) {
            blocked = true;
        }
        check(blocked, "analysis blocked before requirement approval");

        gates.review(state.workItemId(), Stage.REQUIREMENT);
        gates.decide(state.workItemId(), Stage.REQUIREMENT, StageStatus.APPROVED, "approved");
        gates.startStage(state.workItemId(), Stage.ANALYSIS);
        check(repository.load(state.workItemId()).stage(Stage.ANALYSIS).status() == StageStatus.IN_PROGRESS,
                "analysis starts after approval");

        check(new PromptService(root).createContract(repository.load(state.workItemId()), Stage.ANALYSIS)
                .request().promptFile().toFile().isFile(), "classpath prompt fallback");

        Files.createFile(root.resolve("gradlew"));
        check(new TestCommandDetector().detect(root).size() == 2, "gradle test command detection");

        boolean secretRejected = false;
        try {
            SensitiveDataGuard.assertSafeCommand("curl -H 'Authorization: Bearer abcdefghijklmnop'");
        } catch (IllegalArgumentException expected) {
            secretRejected = true;
        }
        check(secretRejected, "credential literal rejection");

        WorkItemState testState = repository.load(state.workItemId());
        testState.addTestCommand(new TestCommand("FAIL", "exit 1", true, 10));
        repository.save(testState);
        ProcessRunner runner = new ProcessRunner();
        TestExecutionService testExecution = new TestExecutionService(
                root,
                repository,
                runner,
                new TestEvidenceWriter(root, new GitService(root, runner))
        );
        TestRunSummary failed = testExecution.execute(state.workItemId(), 3, (attempt, summary) -> true);
        check(!failed.success() && failed.attempts() == 3 && "NEEDS_HUMAN_REVIEW".equals(failed.verdict()),
                "three-attempt failure bound");

        System.out.println("OFFLINE_SMOKE_TEST_PASS");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
