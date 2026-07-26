package com.nh.nsight.marketing.oc.application.dto.env;

public record TimeoutMapNode(
        int order,
        String layer,
        String label,
        long timeoutMs,
        String displayValue,
        AssessmentStatus status,
        String note,
        String sourceFile,
        String propertyKey,
        String configValue,
        String guideValue
) {
}
