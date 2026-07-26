package com.nh.nsight.marketing.oc.application.dto.env;

import java.time.LocalDateTime;
import java.util.List;

public record AssessmentRunView(
        String runId,
        String projectId,
        String envCode,
        LocalDateTime startedAt,
        String status,
        int passCount,
        int warnCount,
        int failCount,
        int totalRules,
        boolean criticalBlocking,
        List<String> configurationCriteria,
        List<AssessmentResultItem> results,
        TimeoutMapView timeoutMap,
        ConcurrentFlowMapView concurrentFlowMap,
        IntegratedEnvironmentView settingsSnapshot
) {
}
