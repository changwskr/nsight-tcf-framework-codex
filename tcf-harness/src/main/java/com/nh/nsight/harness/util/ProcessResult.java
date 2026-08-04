package com.nh.nsight.harness.util;

import java.time.Duration;
import java.util.List;

public record ProcessResult(
        List<String> command,
        int exitCode,
        boolean timedOut,
        Duration duration,
        String stdout,
        String stderr
) {
    public boolean success() {
        return !timedOut && exitCode == 0;
    }
}
