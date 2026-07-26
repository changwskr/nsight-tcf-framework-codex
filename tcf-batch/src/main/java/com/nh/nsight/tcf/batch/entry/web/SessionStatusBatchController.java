package com.nh.nsight.tcf.batch.entry.web;

import com.nh.nsight.tcf.batch.support.model.SessionStatusCollectResult;
import com.nh.nsight.tcf.batch.application.service.SessionStatusCollectService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs/session-status")
public class SessionStatusBatchController {
    private final SessionStatusCollectService collectService;

    public SessionStatusBatchController(SessionStatusCollectService collectService) {
        this.collectService = collectService;
    }

    @GetMapping
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", "BAT-BATCH-003");
        body.put("description", "OM 대시보드 세션 현황 수집 (Spring Session/Actuator → OM_SESSION_STATUS)");
        body.put("runEndpoint", "POST /batch/jobs/session-status/run");
        return body;
    }

    @PostMapping("/run")
    public Map<String, Object> run() {
        SessionStatusCollectResult result = collectService.collectAndPersist();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", result.jobId());
        body.put("runTime", result.runTime());
        body.put("runStatus", result.runStatus());
        body.put("durationMs", result.durationMs());
        body.put("targetCount", result.targetCount());
        body.put("successCount", result.successCount());
        body.put("failCount", result.failCount());
        body.put("totalActiveCount", result.totalActiveCount());
        body.put("totalExpiredCount", result.totalExpiredCount());
        body.put("totalUniqueUsers", result.totalUniqueUsers());
        body.put("message", result.message());
        body.put("snapshots", result.snapshots());
        return body;
    }
}
