package com.nh.nsight.marketing.oc.capnew.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewApprovalCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewApproveRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewRevokeRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioCDTO;
import com.nh.nsight.marketing.oc.capnew.persistence.dao.CapNewApprovalDao;
import com.nh.nsight.marketing.oc.capnew.persistence.dao.CapNewScenarioDao;
import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewApprovalRow;
import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewScenarioRow;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import com.nh.nsight.marketing.oc.capnew.support.CapNewScenarioStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CapNewApprovalService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Pattern VERSION_PATTERN = Pattern.compile("V(\\d+)\\.(\\d+)", Pattern.CASE_INSENSITIVE);

    private final CapNewScenarioDao scenarioDao;
    private final CapNewApprovalDao approvalDao;
    private final CapNewWizardService wizardService;
    private final ObjectMapper objectMapper;

    public CapNewApprovalService(
            CapNewScenarioDao scenarioDao,
            CapNewApprovalDao approvalDao,
            CapNewWizardService wizardService,
            ObjectMapper objectMapper) {
        this.scenarioDao = scenarioDao;
        this.approvalDao = approvalDao;
        this.wizardService = wizardService;
        this.objectMapper = objectMapper;
    }

    public List<CapNewApprovalCDTO> listApprovals() {
        return approvalDao.findAll().stream().map(this::toApprovalDto).toList();
    }

    public List<CapNewApprovalCDTO> listApprovalsByScenario(String scenarioId) {
        return approvalDao.findByScenarioId(scenarioId).stream().map(this::toApprovalDto).toList();
    }

    @Transactional
    public CapNewScenarioCDTO approve(String scenarioId, CapNewApproveRequest request) {
        CapNewScenarioRow row = requireScenario(scenarioId);
        if (!CapNewScenarioStatus.COMPLETED.name().equals(row.getStatus())) {
            throw new CapNewBizException("COMPLETED 상태의 시나리오만 확정할 수 있습니다.");
        }

        Map<String, Object> payload = readPayload(row.getStepPayload());
        if (!payload.containsKey("step8")) {
            throw new CapNewBizException("STEP 8 종합 결과가 없습니다.");
        }

        Map<String, Object> step8 = map(payload.get("step8"));
        String overall = extractOverallJudgment(step8);
        validateApprovalRequest(request, overall, row.getTargetEnv());

        Map<String, Object> approvalMeta = new LinkedHashMap<>();
        approvalMeta.put("approver", request.getApprover());
        approvalMeta.put("reviewer", request.getReviewer());
        approvalMeta.put("approvalNote", request.getApprovalNote());
        approvalMeta.put("approvedAt", LocalDate.now().toString());
        approvalMeta.put("criticalOverride", request.isCriticalOverride());
        step8.put("approval", approvalMeta);
        payload.put("step8", step8);

        row.setStepPayload(writePayload(payload));
        row.setStatus(CapNewScenarioStatus.APPROVED.name());
        scenarioDao.update(row);

        insertApprovalRecord(row, "APPROVE", request.getApprover(), request.getReviewer(),
                request.getApprovalNote(), overall, writeSnapshot(step8));

        return wizardService.getScenario(scenarioId);
    }

    @Transactional
    public CapNewScenarioCDTO revoke(String scenarioId, CapNewRevokeRequest request) {
        CapNewScenarioRow row = requireScenario(scenarioId);
        if (!CapNewScenarioStatus.APPROVED.name().equals(row.getStatus())) {
            throw new CapNewBizException("APPROVED 상태의 시나리오만 확정 취소할 수 있습니다.");
        }

        Map<String, Object> payload = readPayload(row.getStepPayload());
        Map<String, Object> step8 = map(payload.get("step8"));
        String overall = extractOverallJudgment(step8);

        Map<String, Object> approvalMeta = map(step8.get("approval"));
        approvalMeta.put("revokedAt", LocalDate.now().toString());
        approvalMeta.put("revoker", request.getRevoker());
        approvalMeta.put("revokeNote", request.getRevokeNote());
        step8.put("approval", approvalMeta);
        payload.put("step8", step8);

        row.setStepPayload(writePayload(payload));
        row.setStatus(CapNewScenarioStatus.COMPLETED.name());
        scenarioDao.update(row);

        insertApprovalRecord(row, "REVOKE", request.getRevoker(), null,
                request.getRevokeNote(), overall, writeSnapshot(step8));

        return wizardService.getScenario(scenarioId);
    }

    @Transactional
    public CapNewScenarioCDTO cloneVersion(String scenarioId) {
        CapNewScenarioRow source = requireScenario(scenarioId);
        if (!CapNewScenarioStatus.COMPLETED.name().equals(source.getStatus())
                && !CapNewScenarioStatus.APPROVED.name().equals(source.getStatus())) {
            throw new CapNewBizException("COMPLETED 또는 APPROVED 시나리오만 복제할 수 있습니다.");
        }

        String newId = "CAP-NEW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String newVersion = nextVersion(source.getVersionNo());
        Map<String, Object> payload = readPayload(source.getStepPayload());
        if (payload.containsKey("step8")) {
            Map<String, Object> step8 = new LinkedHashMap<>(map(payload.get("step8")));
            step8.remove("approval");
            payload.put("step8", step8);
        }

        CapNewScenarioRow cloned = new CapNewScenarioRow();
        cloned.setScenarioId(newId);
        cloned.setProjectId(source.getProjectId());
        cloned.setProjectName(source.getProjectName());
        cloned.setScenarioName(source.getScenarioName() + " (" + newVersion + ")");
        cloned.setTargetEnv(source.getTargetEnv());
        cloned.setBaseDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        cloned.setVersionNo(newVersion);
        cloned.setAuthor(source.getAuthor());
        cloned.setDescription(source.getDescription());
        cloned.setPurpose(source.getPurpose());
        cloned.setStatus(CapNewScenarioStatus.DRAFT.name());
        cloned.setCurrentStep(8);
        cloned.setStepPayload(writePayload(payload));
        scenarioDao.insert(cloned);

        return wizardService.getScenario(newId);
    }

    private void validateApprovalRequest(CapNewApproveRequest request, String overall, String targetEnv) {
        if (!StringUtils.hasText(request.getApprover())) {
            throw new CapNewBizException("확정자(approver)는 필수입니다.");
        }
        if ("PROD".equalsIgnoreCase(targetEnv) && !StringUtils.hasText(request.getReviewer())) {
            throw new CapNewBizException("운영(PROD) 환경 산정은 검토자(reviewer)가 필요합니다.");
        }
        if ("CRITICAL".equalsIgnoreCase(overall)) {
            if (!request.isCriticalOverride() || !StringUtils.hasText(request.getApprovalNote())) {
                throw new CapNewBizException(
                        "위험(CRITICAL) 판정 시 사유 입력과 criticalOverride=true가 필요합니다.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String extractOverallJudgment(Map<String, Object> step8) {
        Object headline = step8.get("headline");
        if (headline instanceof Map<?, ?> map) {
            Object judgment = ((Map<String, Object>) map).get("overallJudgment");
            if (judgment != null) {
                return String.valueOf(judgment);
            }
        }
        return "NORMAL";
    }

    private void insertApprovalRecord(
            CapNewScenarioRow row,
            String action,
            String approver,
            String reviewer,
            String note,
            String overall,
            String snapshotJson) {
        CapNewApprovalRow approval = new CapNewApprovalRow();
        approval.setApprovalId("APR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        approval.setScenarioId(row.getScenarioId());
        approval.setProjectId(row.getProjectId());
        approval.setScenarioName(row.getScenarioName());
        approval.setVersionNo(row.getVersionNo());
        approval.setAction(action);
        approval.setApprover(approver);
        approval.setReviewer(reviewer);
        approval.setApprovalNote(note);
        approval.setOverallJudgment(overall);
        approval.setSnapshotJson(snapshotJson);
        approvalDao.insert(approval);
    }

    private String writeSnapshot(Map<String, Object> step8) {
        try {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("headline", step8.get("headline"));
            snap.put("conclusion", step8.get("conclusion"));
            snap.put("riskSummary", step8.get("riskSummary"));
            return objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String nextVersion(String current) {
        if (!StringUtils.hasText(current)) {
            return "V1.0";
        }
        Matcher matcher = VERSION_PATTERN.matcher(current.trim());
        if (matcher.matches()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2)) + 1;
            if (minor >= 10) {
                major++;
                minor = 0;
            }
            return "V" + major + "." + minor;
        }
        return current + "-R2";
    }

    private CapNewApprovalCDTO toApprovalDto(CapNewApprovalRow row) {
        CapNewApprovalCDTO dto = new CapNewApprovalCDTO();
        dto.setApprovalId(row.getApprovalId());
        dto.setScenarioId(row.getScenarioId());
        dto.setProjectId(row.getProjectId());
        dto.setScenarioName(row.getScenarioName());
        dto.setVersionNo(row.getVersionNo());
        dto.setAction(row.getAction());
        dto.setApprover(row.getApprover());
        dto.setReviewer(row.getReviewer());
        dto.setApprovalNote(row.getApprovalNote());
        dto.setOverallJudgment(row.getOverallJudgment());
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }

    private CapNewScenarioRow requireScenario(String scenarioId) {
        CapNewScenarioRow row = scenarioDao.findById(scenarioId);
        if (row == null) {
            throw new CapNewBizException("시나리오를 찾을 수 없습니다: " + scenarioId);
        }
        return row;
    }

    private Map<String, Object> readPayload(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new CapNewBizException("시나리오 payload JSON을 읽을 수 없습니다.");
        }
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new CapNewBizException("시나리오 payload JSON을 저장할 수 없습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> m) {
            return new LinkedHashMap<>((Map<String, Object>) m);
        }
        return new LinkedHashMap<>();
    }
}
