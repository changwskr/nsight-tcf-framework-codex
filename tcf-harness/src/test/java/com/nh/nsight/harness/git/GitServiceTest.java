package com.nh.nsight.harness.git;

import com.nh.nsight.harness.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createsDedicatedBranchOnlyFromCleanRepository() throws Exception {
        ProcessRunner runner = new ProcessRunner();
        runner.run(List.of("git", "init", "-b", "main"), tempDir, 30);
        runner.run(List.of("git", "config", "user.name", "Harness Test"), tempDir, 30);
        runner.run(List.of("git", "config", "user.email", "harness@example.invalid"), tempDir, 30);
        Files.writeString(tempDir.resolve("README.md"), "test", StandardCharsets.UTF_8);
        runner.run(List.of("git", "add", "README.md"), tempDir, 30);
        runner.run(List.of("git", "commit", "-m", "initial"), tempDir, 30);
        GitService git = new GitService(tempDir, runner);

        git.createAndSwitchBranch("harness/REQ-20260802-001-test");

        assertThat(git.currentBranch()).isEqualTo("harness/REQ-20260802-001-test");
        assertThat(git.isClean()).isTrue();
    }
}
