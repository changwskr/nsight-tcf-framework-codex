package com.nh.nsight.marketing.oc.application.dto.env;

public record AssessmentResultItem(
        String ruleId,
        String ruleType,
        String domain,
        String severity,
        String description,
        String expectedValue,
        String actualValue,
        AssessmentStatus status,
        String recommendation,
        String source,
        String configFile,
        String propertyKey
) {
}
