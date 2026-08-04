package com.nh.nsight.harness.agent;

import java.time.Duration;
import java.util.List;

public record AgentExecutionResult(
        boolean executed,
        boolean success,
        int exitCode,
        boolean timedOut,
        Duration duration,
        List<String> command,
        String message
) {
    public static AgentExecutionResult skipped(String message) {
        return new AgentExecutionResult(false, false, -1, false, Duration.ZERO, List.of(), message);
    }
}
