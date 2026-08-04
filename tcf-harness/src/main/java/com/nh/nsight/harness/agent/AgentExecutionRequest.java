package com.nh.nsight.harness.agent;

import java.nio.file.Path;
import java.util.Map;

public record AgentExecutionRequest(
        String workItemId,
        String stage,
        Path workingDirectory,
        Path promptFile,
        Path contextFile,
        Path resultFile,
        Path executionFile,
        Path stdoutFile,
        Path stderrFile,
        Map<String, String> placeholders
) {
}
