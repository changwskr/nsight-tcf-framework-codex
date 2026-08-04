package com.nh.nsight.harness.testexec;

import java.util.List;

public record TestRunSummary(boolean success, int attempts, List<TestAttemptResult> results, String verdict) {
}
