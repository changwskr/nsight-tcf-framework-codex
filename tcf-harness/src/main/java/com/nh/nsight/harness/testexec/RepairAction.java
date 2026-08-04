package com.nh.nsight.harness.testexec;

@FunctionalInterface
public interface RepairAction {
    boolean repair(int failedAttempt, TestRunSummary partialSummary) throws Exception;

    static RepairAction disabled() {
        return (attempt, summary) -> false;
    }
}
