package com.nh.nsight.harness.testexec;

import com.nh.nsight.harness.domain.TestCommand;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.storage.JsonStateRepository;
import com.nh.nsight.harness.util.ProcessResult;
import com.nh.nsight.harness.util.ProcessRunner;
import com.nh.nsight.harness.util.SensitiveDataGuard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TestExecutionService {
    private final Path repositoryRoot;
    private final JsonStateRepository repository;
    private final ProcessRunner runner;
    private final TestEvidenceWriter evidenceWriter;

    public TestExecutionService(
            Path repositoryRoot,
            JsonStateRepository repository,
            ProcessRunner runner,
            TestEvidenceWriter evidenceWriter
    ) {
        this.repositoryRoot = repositoryRoot;
        this.repository = repository;
        this.runner = runner;
        this.evidenceWriter = evidenceWriter;
    }

    public TestRunSummary execute(String workItemId, int maxAttempts, RepairAction repairAction) throws Exception {
        WorkItemState state = repository.load(workItemId);
        List<TestCommand> approved = state.testCommands().stream().filter(TestCommand::approved).toList();
        if (approved.isEmpty()) {
            throw new IllegalStateException("No approved test command exists for " + workItemId);
        }
        int boundedAttempts = Math.max(1, Math.min(3, maxAttempts));
        evidenceWriter.writeEnvironment(state);
        evidenceWriter.writeCommands(state);
        List<TestAttemptResult> allResults = new ArrayList<>();

        for (int attempt = 1; attempt <= boundedAttempts; attempt++) {
            boolean attemptSuccess = true;
            Path attemptDir = evidenceWriter.evidenceDirectory(workItemId)
                    .resolve("retry-history").resolve(String.format("attempt-%02d", attempt));
            Files.createDirectories(attemptDir);
            for (TestCommand command : approved) {
                ProcessResult result = runner.runShell(command.command(), repositoryRoot, command.timeoutSeconds());
                Path stdout = attemptDir.resolve(command.id() + "-stdout.log");
                Path stderr = attemptDir.resolve(command.id() + "-stderr.log");
                Files.writeString(stdout, SensitiveDataGuard.redact(result.stdout()), StandardCharsets.UTF_8);
                Files.writeString(stderr, SensitiveDataGuard.redact(result.stderr()), StandardCharsets.UTF_8);
                TestAttemptResult attemptResult = new TestAttemptResult(
                        attempt, command.id(), command.command(), result.success(), result.exitCode(),
                        result.timedOut(), result.duration(), stdout, stderr);
                allResults.add(attemptResult);
                if (!result.success()) {
                    attemptSuccess = false;
                    break;
                }
            }
            if (attemptSuccess) {
                TestRunSummary summary = new TestRunSummary(true, attempt, List.copyOf(allResults), "PASS");
                evidenceWriter.writeSummary(state, summary);
                return summary;
            }
            TestRunSummary partial = new TestRunSummary(false, attempt, List.copyOf(allResults), "RETRY_PENDING");
            if (attempt >= boundedAttempts || !repairAction.repair(attempt, partial)) {
                String verdict = attempt >= boundedAttempts ? "NEEDS_HUMAN_REVIEW" : "FAIL";
                TestRunSummary summary = new TestRunSummary(false, attempt, List.copyOf(allResults), verdict);
                evidenceWriter.writeSummary(state, summary);
                return summary;
            }
        }
        throw new IllegalStateException("Unreachable test execution state");
    }
}
