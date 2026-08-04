package com.nh.nsight.harness.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessCommandRouterTest {
    @TempDir
    Path tempDir;

    @Test
    void printsCommandHelp() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HarnessCommandRouter router = new HarnessCommandRouter(
                new PrintStream(output, true, StandardCharsets.UTF_8), System.err);

        int exit = router.run(new String[]{"help"});

        assertThat(exit).isZero();
        assertThat(output.toString(StandardCharsets.UTF_8)).contains("requirement", "analyze", "test run");
    }

    @Test
    void initializesWorkItemWithoutBranchForControlledTest() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HarnessCommandRouter router = new HarnessCommandRouter(
                new PrintStream(output, true, StandardCharsets.UTF_8), System.err);

        int exit = router.run(new String[]{
                "init", "--repo", tempDir.toString(), "--id", "REQ-20260802-001",
                "--title", "고객 조회", "--skip-branch"
        });

        assertThat(exit).isZero();
        assertThat(tempDir.resolve(".harness/state/REQ-20260802-001.json")).exists();
        assertThat(tempDir.resolve("docs/work-items/REQ-20260802-001/README.md")).exists();
    }
}
