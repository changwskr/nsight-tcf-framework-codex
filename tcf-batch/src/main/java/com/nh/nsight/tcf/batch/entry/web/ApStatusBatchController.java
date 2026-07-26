package com.nh.nsight.tcf.batch.entry.web;

import com.nh.nsight.tcf.batch.support.model.ApStatusCollectResult;
import com.nh.nsight.tcf.batch.application.service.ApStatusCollectService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs/ap-status")
public class ApStatusBatchController {
    private final ApStatusCollectService collectService;

    public ApStatusBatchController(ApStatusCollectService collectService) {
        this.collectService = collectService;
    }

    @GetMapping
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", "BAT-BATCH-001");
        body.put("description", "OM 대시보드 AP 상태 수집 (Actuator → OM_AP_STATUS)");
        body.put("runEndpoint", "POST /batch/jobs/ap-status/run");
        return body;
    }

    @PostMapping("/run")
    public Map<String, Object> run() {
        ApStatusCollectResult result = collectService.collectAndPersist();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", result.jobId());
        body.put("runTime", result.runTime());
        body.put("runStatus", result.runStatus());
        body.put("durationMs", result.durationMs());
        body.put("targetCount", result.targetCount());
        body.put("successCount", result.successCount());
        body.put("failCount", result.failCount());
        body.put("message", result.message());
        body.put("snapshots", result.snapshots());
        return body;
    }
}
