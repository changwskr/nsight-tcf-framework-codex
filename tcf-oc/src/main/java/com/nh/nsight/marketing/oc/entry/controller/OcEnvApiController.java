package com.nh.nsight.marketing.oc.entry.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nh.nsight.marketing.oc.application.dto.env.AssessmentResultItem;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentRunView;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityDesignView;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerRequest;
import com.nh.nsight.marketing.oc.application.dto.env.ConcurrentFlowMapView;
import com.nh.nsight.marketing.oc.application.dto.env.ConfigImportResult;
import com.nh.nsight.marketing.oc.application.dto.env.IntegratedEnvironmentView;
import com.nh.nsight.marketing.oc.application.dto.env.OcEnvApiResponse;
import com.nh.nsight.marketing.oc.application.dto.env.ProjectBaselineView;
import com.nh.nsight.marketing.oc.application.dto.env.TimeoutMapView;
import com.nh.nsight.marketing.oc.application.dto.env.TraceEnvironmentExportRequest;
import com.nh.nsight.marketing.oc.application.service.env.CapacityDesignService;
import com.nh.nsight.marketing.oc.application.service.env.ConfigImportService;
import com.nh.nsight.marketing.oc.application.service.env.EnvironmentAssessmentService;
import com.nh.nsight.marketing.oc.application.service.env.ProjectBaselineService;
import com.nh.nsight.marketing.oc.application.service.env.TraceEnvironmentExcelExportService;
import com.nh.nsight.marketing.oc.application.service.env.TraceEnvironmentService;

@RestController
@RequestMapping("/api/oc/env")
public class OcEnvApiController {

    private final TraceEnvironmentService traceEnvironmentService;
    private final ProjectBaselineService projectBaselineService;
    private final ConfigImportService configImportService;
    private final EnvironmentAssessmentService assessmentService;
    private final CapacityDesignService capacityDesignService;
    private final TraceEnvironmentExcelExportService excelExportService;

    public OcEnvApiController(
            TraceEnvironmentService traceEnvironmentService,
            ProjectBaselineService projectBaselineService,
            ConfigImportService configImportService,
            EnvironmentAssessmentService assessmentService,
            CapacityDesignService capacityDesignService,
            TraceEnvironmentExcelExportService excelExportService) {
        this.traceEnvironmentService = traceEnvironmentService;
        this.projectBaselineService = projectBaselineService;
        this.configImportService = configImportService;
        this.assessmentService = assessmentService;
        this.capacityDesignService = capacityDesignService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/capacity-design/defaults")
    public OcEnvApiResponse<CapacityPlannerRequest> capacityDesignDefaults() {
        return OcEnvApiResponse.success("ENV-CAP-DEF-001", "capacityDesignDefaults",
                capacityDesignService.defaultRequest());
    }

    @PostMapping("/capacity-design/analyze")
    public OcEnvApiResponse<CapacityDesignView> analyzeCapacityDesign(@RequestBody CapacityPlannerRequest request) {
        return OcEnvApiResponse.success("ENV-CAP-AN-001", "capacityDesignAnalyze",
                capacityDesignService.analyze(request));
    }

    @GetMapping("/settings")
    public OcEnvApiResponse<IntegratedEnvironmentView> settings() {
        return OcEnvApiResponse.success("ENV-SET-001", "integratedEnvironmentSettings",
                traceEnvironmentService.loadIntegratedSettings());
    }

    @GetMapping("/projects/baseline")
    public OcEnvApiResponse<ProjectBaselineView> baseline(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String envCode) {
        return OcEnvApiResponse.success("ENV-BASE-001", "projectBaseline",
                projectBaselineService.loadBaseline(projectId, envCode));
    }

    @PostMapping("/config-files/upload")
    public OcEnvApiResponse<ConfigImportResult> uploadConfigFiles(@RequestParam("files") MultipartFile[] files)
            throws Exception {
        return OcEnvApiResponse.success("ENV-UPL-001", "configFileUpload",
                configImportService.importFiles(files));
    }

    @PostMapping("/assessments")
    public OcEnvApiResponse<AssessmentRunView> runAssessment(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String envCode,
            @RequestParam(defaultValue = "true") boolean mergeUploaded) {
        return OcEnvApiResponse.success("ENV-ASM-001", "environmentAssessment",
                assessmentService.runAssessment(projectId, envCode, mergeUploaded));
    }

    @GetMapping("/assessments/{runId}")
    public OcEnvApiResponse<AssessmentRunView> getAssessment(@PathVariable String runId) {
        return OcEnvApiResponse.success("ENV-ASM-GET-001", "environmentAssessmentRun",
                assessmentService.getRun(runId));
    }

    @GetMapping("/assessments/{runId}/results")
    public OcEnvApiResponse<List<AssessmentResultItem>> getResults(@PathVariable String runId) {
        return OcEnvApiResponse.success("ENV-ASM-RES-001", "assessmentResults",
                assessmentService.getRun(runId).results());
    }

    @GetMapping("/assessments/{runId}/timeout-map")
    public OcEnvApiResponse<TimeoutMapView> getTimeoutMap(@PathVariable String runId) {
        return OcEnvApiResponse.success("ENV-ASM-TMO-001", "timeoutMap",
                assessmentService.getRun(runId).timeoutMap());
    }

    @GetMapping("/assessments/{runId}/concurrent-flow-map")
    public OcEnvApiResponse<ConcurrentFlowMapView> getConcurrentFlowMap(@PathVariable String runId) {
        return OcEnvApiResponse.success("ENV-ASM-CAP-001", "concurrentFlowMap",
                assessmentService.getRun(runId).concurrentFlowMap());
    }

    @PostMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(@RequestBody TraceEnvironmentExportRequest request) throws Exception {
        byte[] body = excelExportService.export(request);
        String filename = excelExportService.filename(request);
        String encoded = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/dashboard/summary")
    public OcEnvApiResponse<Map<String, Object>> dashboardSummary(@RequestParam(required = false) String runId) {
        if (runId == null || runId.isBlank()) {
            var settings = traceEnvironmentService.loadIntegratedSettings();
            return OcEnvApiResponse.success("ENV-DSH-001", "dashboardSummary", Map.of(
                    "mode", "settings-only",
                    "matchCount", settings.matchCount(),
                    "warnCount", settings.warnCount(),
                    "totalCompared", settings.totalCompared()));
        }
        AssessmentRunView run = assessmentService.getRun(runId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", run.runId());
        summary.put("status", run.status());
        summary.put("passCount", run.passCount());
        summary.put("warnCount", run.warnCount());
        summary.put("failCount", run.failCount());
        summary.put("criticalBlocking", run.criticalBlocking());
        summary.put("timeoutChainValid", run.timeoutMap().chainValid());
        summary.put("concurrentFlowValid", run.concurrentFlowMap().chainValid());
        summary.put("actualRequestUsersTotal", run.concurrentFlowMap().actualRequestUsersTotal());
        summary.put("estimatedConcurrentPerAp", run.concurrentFlowMap().estimatedConcurrentPerAp());
        summary.put("peakTpsFromActualRequest", run.concurrentFlowMap().peakTpsFromActualRequest());
        return OcEnvApiResponse.success("ENV-DSH-001", "dashboardSummary", summary);
    }
}
