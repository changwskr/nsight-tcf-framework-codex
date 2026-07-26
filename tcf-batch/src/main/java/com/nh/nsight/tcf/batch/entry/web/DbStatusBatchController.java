package com.nh.nsight.tcf.batch.entry.web;

import com.nh.nsight.tcf.batch.support.model.DbStatusCollectResult;
import com.nh.nsight.tcf.batch.application.service.DbStatusCollectService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs/db-status")
public class DbStatusBatchController {
    private final DbStatusCollectService collectService;

    public DbStatusBatchController(DbStatusCollectService collectService) {
        this.collectService = collectService;
    }

    @GetMapping
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", "BAT-BATCH-002");
        body.put("description", "OM 대시보드 DB 상태 수집 (Actuator/JDBC → OM_DB_STATUS)");
        body.put("runEndpoint", "POST /batch/jobs/db-status/run");
        return body;
    }

    @PostMapping("/run")
    public Map<String, Object> run() {
        DbStatusCollectResult result = collectService.collectAndPersist();
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
