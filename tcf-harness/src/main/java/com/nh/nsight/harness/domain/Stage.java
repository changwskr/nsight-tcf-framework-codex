package com.nh.nsight.harness.domain;

import java.util.Optional;

public enum Stage {
    REQUIREMENT,
    ANALYSIS,
    DESIGN,
    IMPLEMENTATION,
    TEST,
    CLOSE;

    public Optional<Stage> prerequisite() {
        return switch (this) {
            case REQUIREMENT -> Optional.empty();
            case ANALYSIS -> Optional.of(REQUIREMENT);
            case DESIGN -> Optional.of(ANALYSIS);
            case IMPLEMENTATION -> Optional.of(DESIGN);
            case TEST -> Optional.of(IMPLEMENTATION);
            case CLOSE -> Optional.of(TEST);
        };
    }

    public String artifactFileName() {
        return switch (this) {
            case REQUIREMENT -> "requirement.md";
            case ANALYSIS -> "analysis.md";
            case DESIGN -> "design.md";
            case IMPLEMENTATION -> "implementation-result.md";
            case TEST -> "test-evidence/test-summary.md";
            case CLOSE -> "closure.md";
        };
    }
}
