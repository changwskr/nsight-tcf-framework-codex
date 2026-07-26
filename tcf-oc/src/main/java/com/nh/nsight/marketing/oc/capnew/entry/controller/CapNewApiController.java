package com.nh.nsight.marketing.oc.capnew.entry.controller;

import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewApiResponse;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCreateScenarioRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewDefaultsCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioSummaryCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioTemplateCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewStepSaveRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCompareCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCompareRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewApprovalCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewApproveRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewRevokeRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewEnvHandoffCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewExcelExportRequest;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewApprovalService;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewCompareService;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewDefaultsService;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewEnvBridgeService;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewLegacyCompareCDTO;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewExcelExportService;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewVmCompareCDTO;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewLegacyCompareService;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewVmCompareService;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewScenarioTemplateService;
import com.nh.nsight.marketing.oc.capnew.application.service.CapNewWizardService;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oc/cap-new")
public class CapNewApiController {

    private static final String AC = "CapNewApiController";

    private final CapNewDefaultsService defaultsService;
    private final CapNewWizardService wizardService;
    private final CapNewScenarioTemplateService templateService;
    private final CapNewCompareService compareService;
    private final CapNewApprovalService approvalService;
    private final CapNewEnvBridgeService envBridgeService;
    private final CapNewExcelExportService excelExportService;
    private final CapNewLegacyCompareService legacyCompareService;
    private final CapNewVmCompareService vmCompareService;

    public CapNewApiController(
            CapNewDefaultsService defaultsService,
            CapNewWizardService wizardService,
            CapNewScenarioTemplateService templateService,
            CapNewCompareService compareService,
            CapNewApprovalService approvalService,
            CapNewEnvBridgeService envBridgeService,
            CapNewExcelExportService excelExportService,
            CapNewLegacyCompareService legacyCompareService,
            CapNewVmCompareService vmCompareService) {
        this.defaultsService = defaultsService;
        this.wizardService = wizardService;
        this.templateService = templateService;
        this.compareService = compareService;
        this.approvalService = approvalService;
        this.envBridgeService = envBridgeService;
        this.excelExportService = excelExportService;
        this.legacyCompareService = legacyCompareService;
        this.vmCompareService = vmCompareService;
    }

    @GetMapping("/defaults")
    public ResponseEntity<CapNewApiResponse<CapNewDefaultsCDTO>> defaults() {
        System.out.println("★★★★★ [" + AC + "] defaults");
        return ResponseEntity.ok(CapNewApiResponse.ok(defaultsService.defaults(), "NEW 용량산정 기본값"));
    }

    @GetMapping("/templates")
    public ResponseEntity<CapNewApiResponse<List<CapNewScenarioTemplateCDTO>>> listTemplates() {
        System.out.println("★★★★★ [" + AC + "] listTemplates");
        return ResponseEntity.ok(CapNewApiResponse.ok(templateService.listTemplates(), "시나리오 템플릿 목록"));
    }

    @GetMapping("/templates/{code}")
    public ResponseEntity<CapNewApiResponse<CapNewScenarioTemplateCDTO>> getTemplate(
            @PathVariable String code) {
        System.out.println("★★★★★ [" + AC + "] getTemplate code=" + code);
        return ResponseEntity.ok(CapNewApiResponse.ok(templateService.getTemplate(code), "시나리오 템플릿"));
    }

    @GetMapping("/scenarios")
    public ResponseEntity<CapNewApiResponse<List<CapNewScenarioSummaryCDTO>>> listScenarios(
            @RequestParam(value = "status", required = false) String status) {
        System.out.println("★★★★★ [" + AC + "] listScenarios status=" + status);
        return ResponseEntity.ok(CapNewApiResponse.ok(wizardService.listScenarios(status), "시나리오 목록"));
    }

    @PostMapping("/compare")
    public ResponseEntity<CapNewApiResponse<CapNewCompareCDTO>> compare(
            @RequestBody CapNewCompareRequest request) {
        System.out.println("★★★★★ [" + AC + "] compare ids=" + request.getScenarioIds());
        return ResponseEntity.ok(CapNewApiResponse.ok(compareService.compare(request), "시나리오 비교"));
    }

    @PostMapping("/scenarios")
    public ResponseEntity<CapNewApiResponse<CapNewScenarioCDTO>> createScenario(
            @RequestBody CapNewCreateScenarioRequest request) {
        System.out.println("★★★★★ [" + AC + "] createScenario");
        CapNewScenarioCDTO created = wizardService.createScenario(request);
        return ResponseEntity.ok(CapNewApiResponse.ok(created, "시나리오가 생성되었습니다."));
    }

    @GetMapping("/scenarios/{scenarioId}")
    public ResponseEntity<CapNewApiResponse<CapNewScenarioCDTO>> getScenario(
            @PathVariable String scenarioId) {
        System.out.println("★★★★★ [" + AC + "] getScenario id=" + scenarioId);
        return ResponseEntity.ok(CapNewApiResponse.ok(wizardService.getScenario(scenarioId), "시나리오 상세"));
    }

    @PutMapping("/scenarios/{scenarioId}/step/{stepNumber}")
    public ResponseEntity<CapNewApiResponse<CapNewScenarioCDTO>> saveStep(
            @PathVariable String scenarioId,
            @PathVariable int stepNumber,
            @RequestBody CapNewStepSaveRequest request) {
        System.out.println("★★★★★ [" + AC + "] saveStep id=" + scenarioId + " step=" + stepNumber);
        CapNewScenarioCDTO saved = wizardService.saveStep(scenarioId, stepNumber, request);
        return ResponseEntity.ok(CapNewApiResponse.ok(saved, "STEP " + stepNumber + " 저장 완료"));
    }

    @PostMapping("/scenarios/{scenarioId}/approve")
    public ResponseEntity<CapNewApiResponse<CapNewScenarioCDTO>> approve(
            @PathVariable String scenarioId,
            @RequestBody CapNewApproveRequest request) {
        System.out.println("★★★★★ [" + AC + "] approve id=" + scenarioId);
        return ResponseEntity.ok(CapNewApiResponse.ok(
                approvalService.approve(scenarioId, request), "시나리오가 확정되었습니다."));
    }

    @PostMapping("/scenarios/{scenarioId}/revoke")
    public ResponseEntity<CapNewApiResponse<CapNewScenarioCDTO>> revoke(
            @PathVariable String scenarioId,
            @RequestBody CapNewRevokeRequest request) {
        System.out.println("★★★★★ [" + AC + "] revoke id=" + scenarioId);
        return ResponseEntity.ok(CapNewApiResponse.ok(
                approvalService.revoke(scenarioId, request), "확정이 취소되었습니다."));
    }

    @PostMapping("/scenarios/{scenarioId}/clone")
    public ResponseEntity<CapNewApiResponse<CapNewScenarioCDTO>> cloneVersion(
            @PathVariable String scenarioId) {
        System.out.println("★★★★★ [" + AC + "] clone id=" + scenarioId);
        return ResponseEntity.ok(CapNewApiResponse.ok(
                approvalService.cloneVersion(scenarioId), "새 버전 시나리오가 생성되었습니다."));
    }

    @GetMapping("/approvals")
    public ResponseEntity<CapNewApiResponse<List<CapNewApprovalCDTO>>> listApprovals() {
        System.out.println("★★★★★ [" + AC + "] listApprovals");
        return ResponseEntity.ok(CapNewApiResponse.ok(approvalService.listApprovals(), "확정 이력"));
    }

    @GetMapping("/scenarios/{scenarioId}/approvals")
    public ResponseEntity<CapNewApiResponse<List<CapNewApprovalCDTO>>> listScenarioApprovals(
            @PathVariable String scenarioId) {
        System.out.println("★★★★★ [" + AC + "] listScenarioApprovals id=" + scenarioId);
        return ResponseEntity.ok(CapNewApiResponse.ok(
                approvalService.listApprovalsByScenario(scenarioId), "시나리오 확정 이력"));
    }

    @DeleteMapping("/scenarios/{scenarioId}")
    public ResponseEntity<CapNewApiResponse<Map<String, String>>> deleteScenario(
            @PathVariable String scenarioId) {
        System.out.println("★★★★★ [" + AC + "] deleteScenario id=" + scenarioId);
        wizardService.deleteScenario(scenarioId);
        return ResponseEntity.ok(CapNewApiResponse.ok(Map.of("scenarioId", scenarioId), "시나리오가 삭제되었습니다."));
    }

    @GetMapping("/scenarios/{scenarioId}/env-handoff")
    public ResponseEntity<CapNewApiResponse<CapNewEnvHandoffCDTO>> envHandoff(
            @PathVariable String scenarioId) {
        System.out.println("★★★★★ [" + AC + "] envHandoff id=" + scenarioId);
        return ResponseEntity.ok(CapNewApiResponse.ok(
                envBridgeService.buildHandoff(scenarioId), "ENV-002 연동 데이터"));
    }

    @GetMapping("/scenarios/{scenarioId}/legacy-compare")
    public ResponseEntity<CapNewApiResponse<CapNewLegacyCompareCDTO>> legacyCompare(
            @PathVariable String scenarioId) {
        System.out.println("★★★★★ [" + AC + "] legacyCompare id=" + scenarioId);
        return ResponseEntity.ok(CapNewApiResponse.ok(
                legacyCompareService.compare(scenarioId), "기존 CAP 대조 결과"));
    }

    @GetMapping("/scenarios/{scenarioId}/vm-compare")
    public ResponseEntity<CapNewApiResponse<CapNewVmCompareCDTO>> vmCompare(
            @PathVariable String scenarioId,
            @RequestParam(value = "profiles", required = false) String profiles) {
        System.out.println("★★★★★ [" + AC + "] vmCompare id=" + scenarioId + " profiles=" + profiles);
        List<String> profileList = parseProfiles(profiles);
        return ResponseEntity.ok(CapNewApiResponse.ok(
                vmCompareService.compare(scenarioId, profileList), "VM 대안 비교 결과"));
    }

    private List<String> parseProfiles(String profiles) {
        if (profiles == null || profiles.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(profiles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @PostMapping(value = "/scenarios/{scenarioId}/export/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportScenarioExcel(@PathVariable String scenarioId) throws Exception {
        System.out.println("★★★★★ [" + AC + "] exportScenarioExcel id=" + scenarioId);
        CapNewExcelExportRequest request = new CapNewExcelExportRequest();
        request.setExportType("SCENARIO");
        request.setScenarioId(scenarioId);
        return excelResponse(request);
    }

    @PostMapping(value = "/export/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(@RequestBody CapNewExcelExportRequest request) throws Exception {
        System.out.println("★★★★★ [" + AC + "] exportExcel type=" + request.getExportType());
        return excelResponse(request);
    }

    private ResponseEntity<byte[]> excelResponse(CapNewExcelExportRequest request) throws Exception {
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

    @ExceptionHandler(CapNewBizException.class)
    public ResponseEntity<CapNewApiResponse<Void>> handleBiz(CapNewBizException ex) {
        System.out.println("★★★★★ [" + AC + "] handleBiz " + ex.getMessage());
        return ResponseEntity.badRequest().body(CapNewApiResponse.fail(ex.getMessage()));
    }
}
