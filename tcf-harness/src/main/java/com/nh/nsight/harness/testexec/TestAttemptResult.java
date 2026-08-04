package com.nh.nsight.harness.testexec;

import java.nio.file.Path;
import java.time.Duration;

public record TestAttemptResult(
        int attempt,
        String commandId,
        String command,
        boolean success,
        int exitCode,
        boolean timedOut,
        Duration duration,
        Path stdoutFile,
        Path stderrFile
) {
}
