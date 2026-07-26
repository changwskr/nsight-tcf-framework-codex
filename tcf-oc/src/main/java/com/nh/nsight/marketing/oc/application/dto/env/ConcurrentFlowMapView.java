package com.nh.nsight.marketing.oc.application.dto.env;

import java.util.List;

/**
 * 실시간 동시 요청자(용량) 계층 점검 — SC-009.
 */
public record ConcurrentFlowMapView(
        String runId,
        String chainRuleId,
        boolean chainValid,
        String chainSummary,
        int actualRequestUsersTotal,
        int estimatedConcurrentPerAp,
        int peakTpsFromActualRequest,
        int configuredPeakTps,
        List<ConcurrentFlowMapNode> nodes
) {
}
