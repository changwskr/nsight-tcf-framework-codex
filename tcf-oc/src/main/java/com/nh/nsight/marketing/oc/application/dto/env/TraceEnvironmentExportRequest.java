package com.nh.nsight.marketing.oc.application.dto.env;

/** 브라우저 산정·점검 캐시를 Excel로보낼 때 POST body. */
public record TraceEnvironmentExportRequest(
        String exportType,
        CapacityDesignView capacityView,
        CapacityPlannerRequest capacityRequest,
        AssessmentRunView assessmentRun,
        ProjectBaselineView baseline
) {
}
