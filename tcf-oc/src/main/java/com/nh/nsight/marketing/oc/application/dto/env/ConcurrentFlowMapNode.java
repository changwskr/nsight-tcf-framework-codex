package com.nh.nsight.marketing.oc.application.dto.env;

public record ConcurrentFlowMapNode(
        int order,
        String layer,
        String label,
        long capacityValue,
        String displayValue,
        AssessmentStatus status,
        String note,
        String sourceFile,
        String propertyKey,
        String configValue,
        String guideValue
) {
}
