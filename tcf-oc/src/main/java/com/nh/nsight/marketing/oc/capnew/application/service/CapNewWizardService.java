package com.nh.nsight.marketing.oc.capnew.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCascadeImpactCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCreateScenarioRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioSummaryCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewStepSaveRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewStepTrackStatusCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewStepValidationCDTO;
import com.nh.nsight.marketing.oc.capnew.application.rule.CapNewStepRule;
import com.nh.nsight.marketing.oc.capnew.persistence.dao.CapNewScenarioDao;
import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewScenarioRow;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import com.nh.nsight.marketing.oc.capnew.support.CapNewScenarioStatus;
import com.nh.nsight.marketing.oc.capnew.support.CapNewStep;
import com.nh.nsight.marketing.oc.capnew.support.CapNewStepSnapshot;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CapNewWizardService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final CapNewScenarioDao scenarioDao;
    private final CapNewStepRule stepRule;
    private final CapNewDerivationService derivationService;
    private final CapNewScenarioTemplateService templateService;
    private final ObjectMapper objectMapper;

    public CapNewWizardService(
            CapNewScenarioDao scenarioDao,
            CapNewStepRule stepRule,
            CapNewDerivationService derivationService,
            CapNewScenarioTemplateService templateService,
            ObjectMapper objectMapper) {
        this.scenarioDao = scenarioDao;
        this.stepRule = stepRule;
        this.derivationService = derivationService;
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    public List<CapNewScenarioSummaryCDTO> listScenarios() {
        return listScenarios(null);
    }

    public List<CapNewScenarioSummaryCDTO> listScenarios(String status) {
        return scenarioDao.findAll().stream()
                .filter(row -> !StringUtils.hasText(status) || status.equalsIgnoreCase(row.getStatus()))
                .map(this::toSummary)
                .toList();
    }

    public CapNewScenarioCDTO getScenario(String scenarioId) {
        CapNewScenarioRow row = requireScenario(scenarioId);
        return toDetail(row);
    }

    @Transactional
    public CapNewScenarioCDTO createScenario(CapNewCreateScenarioRequest request) {
        Map<String, Object> payload;
        int currentStep;

        if (StringUtils.hasText(request.getTemplateCode())) {
            Map<String, Object> seed = templateService.getSeedPayload(request.getTemplateCode());
            payload = materializeSeedPayload(seed);
            mergeStep1Overrides(payload, request);
            currentStep = resolveHighestStep(payload);
        } else {
            CapNewStepValidationCDTO validation = stepRule.validateStep1(request.toStep1Map());
            if (!validation.isValid()) {
                throw new CapNewBizException(String.join(", ", validation.getErrors()));
            }
            payload = new LinkedHashMap<>();
            payload.put("step1", request.toStep1Map());
            currentStep = 1;
        }

        String scenarioId = "CAP-NEW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        CapNewScenarioRow row = new CapNewScenarioRow();
        row.setScenarioId(scenarioId);
        row.setProjectId(request.getProjectId());
        row.setProjectName(request.getProjectName());
        row.setScenarioName(request.getScenarioName());
        row.setTargetEnv(request.getTargetEnv());
        row.setBaseDate(resolveBaseDate(request.getBaseDate()));
        row.setVersionNo(StringUtils.hasText(request.getVersionNo()) ? request.getVersionNo() : "V1.0");
        row.setAuthor(request.getAuthor());
        row.setDescription(request.getDescription());
        row.setPurpose(request.getPurpose());
        row.setStatus(CapNewScenarioStatus.DRAFT.name());
        row.setCurrentStep(currentStep);
        row.setStepPayload(writePayload(payload));
        applyMetadataFromStep1(row, payload);
        scenarioDao.insert(row);
        return toDetail(row);
    }

    public Map<String, Object> materializeSeedPayload(Map<String, Object> seed) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int stepNum = 1; stepNum <= 7; stepNum++) {
            String key = "step" + stepNum;
            Object raw = seed.get(key);
            if (!(raw instanceof Map<?, ?>)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> stepPayload = new LinkedHashMap<>((Map<String, Object>) raw);
            if (stepNum == 6) {
                alignStep6Baseline(payload, stepPayload);
            }
            CapNewStep step = CapNewStep.resolve(stepNum);
            CapNewStepValidationCDTO validation = validateAndEnrich(step, payload, stepPayload);
            if (!validation.isValid()) {
                throw new CapNewBizException("템플릿 STEP " + stepNum + " 검증 실패: "
                        + String.join(", ", validation.getErrors()));
            }
            payload.put(key, stepPayload);
        }
        if (seed.containsKey("_templateCode")) {
            payload.put("_templateCode", seed.get("_templateCode"));
        }
        if (seed.containsKey("_templateLabel")) {
            payload.put("_templateLabel", seed.get("_templateLabel"));
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    private void mergeStep1Overrides(Map<String, Object> payload, CapNewCreateScenarioRequest request) {
        Map<String, Object> step1 = payload.get("step1") instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map)
                : new LinkedHashMap<>();
        Map<String, Object> overrides = request.toStep1Map();
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            Object value = entry.getValue();
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                step1.put(entry.getKey(), value);
            }
        }
        payload.put("step1", step1);
    }

    @SuppressWarnings("unchecked")
    private void alignStep6Baseline(Map<String, Object> payload, Map<String, Object> step6) {
        Object step3Raw = payload.get("step3");
        if (!(step3Raw instanceof Map<?, ?> step3Map)) {
            return;
        }
        Object baseline = ((Map<String, Object>) step3Map).get("operatingBaseline");
        if (baseline != null && StringUtils.hasText(String.valueOf(baseline))) {
            step6.put("baselineScenarioCode", String.valueOf(baseline).trim());
        }
    }

    private int resolveHighestStep(Map<String, Object> payload) {
        int highest = 1;
        for (int stepNum = 1; stepNum <= 7; stepNum++) {
            if (hasStepData(payload, stepNum)) {
                highest = stepNum;
            }
        }
        return highest;
    }

    @Transactional
    public CapNewScenarioCDTO saveStep(String scenarioId, int stepNumber, CapNewStepSaveRequest request) {
        CapNewStep step = CapNewStep.resolve(stepNumber);
        CapNewScenarioRow row = requireScenario(scenarioId);
        if (CapNewScenarioStatus.APPROVED.name().equals(row.getStatus())) {
            throw new CapNewBizException("확정된 시나리오는 수정할 수 없습니다.");
        }

        Map<String, Object> payload = readPayload(row.getStepPayload());
        Map<String, String> beforeSnapshot = CapNewStepSnapshot.extract(payload);
        Map<String, Object> stepPayload = request.getPayload() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getPayload());
        CapNewStepValidationCDTO validation = validateAndEnrich(step, payload, stepPayload);

        if (!validation.isValid()) {
            throw new CapNewBizException(String.join(", ", validation.getErrors()));
        }

        payload.put("step" + stepNumber, stepPayload);
        List<String> cascadeWarnings = new ArrayList<>();
        CapNewCascadeImpactCDTO cascadeImpact = cascadeDownstream(stepNumber, payload, beforeSnapshot, cascadeWarnings);
        for (String warning : cascadeWarnings) {
            validation.addWarning(warning);
        }

        row.setStepPayload(writePayload(payload));
        applyMetadataFromStep1(row, payload);
        row.setCurrentStep(Math.max(row.getCurrentStep(), stepNumber));
        if (payload.containsKey("step8")) {
            row.setStatus(CapNewScenarioStatus.COMPLETED.name());
        } else if (stepNumber >= 8) {
            row.setStatus(CapNewScenarioStatus.COMPLETED.name());
        }
        scenarioDao.update(row);

        CapNewScenarioCDTO detail = toDetail(row);
        detail.setLastValidation(validation);
        detail.setCascadeImpact(cascadeImpact);
        return detail;
    }

    @Transactional
    public void deleteScenario(String scenarioId) {
        CapNewScenarioRow row = requireScenario(scenarioId);
        if (!CapNewScenarioStatus.DRAFT.name().equals(row.getStatus())) {
            throw new CapNewBizException("초안(DRAFT) 상태의 시나리오만 삭제할 수 있습니다.");
        }
        scenarioDao.deleteById(scenarioId);
    }

    private CapNewStepValidationCDTO validateAndEnrich(
            CapNewStep step,
            Map<String, Object> allPayload,
            Map<String, Object> stepPayload) {
        return switch (step) {
            case BASIC -> stepRule.validateStep1(stepPayload);
            case USER_SESSION -> {
                Map<String, Object> enriched = stepRule.enrichStep2(stepPayload);
                stepPayload.clear();
                stepPayload.putAll(enriched);
                yield stepRule.validateStep2(stepPayload);
            }
            case TPS -> {
                int totalUsers = resolveTotalUsers(allPayload);
                Map<String, Object> enriched = stepRule.enrichStep3(stepPayload, totalUsers);
                stepPayload.clear();
                stepPayload.putAll(enriched);
                yield stepRule.validateStep3(stepPayload);
            }
            case VM -> {
                requirePriorSteps(allPayload, 3);
                Map<String, Object> enriched = derivationService.enrichStep4(stepPayload, allPayload);
                stepPayload.clear();
                stepPayload.putAll(enriched);
                yield stepRule.validateStep4(stepPayload);
            }
            case AP_DR -> {
                requirePriorSteps(allPayload, 4);
                Map<String, Object> enriched = derivationService.enrichStep5(stepPayload, allPayload);
                stepPayload.clear();
                stepPayload.putAll(enriched);
                yield stepRule.validateStep5(stepPayload);
            }
            case WAS -> {
                requirePriorSteps(allPayload, 5);
                Map<String, Object> enriched = derivationService.enrichStep6(stepPayload, allPayload);
                stepPayload.clear();
                stepPayload.putAll(enriched);
                yield stepRule.validateStep6(stepPayload);
            }
            case DB_POOL -> {
                requirePriorSteps(allPayload, 6);
                Map<String, Object> enriched = derivationService.enrichStep7(stepPayload, allPayload);
                stepPayload.clear();
                stepPayload.putAll(enriched);
                yield stepRule.validateStep7(stepPayload);
            }
            case SUMMARY -> {
                requirePriorSteps(allPayload, 7);
                Map<String, Object> enriched = derivationService.enrichStep8(stepPayload, allPayload);
                stepPayload.clear();
                stepPayload.putAll(enriched);
                yield stepRule.validateStep8(stepPayload);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private CapNewCascadeImpactCDTO cascadeDownstream(
            int savedStep,
            Map<String, Object> payload,
            Map<String, String> beforeSnapshot,
            List<String> warnings) {
        CapNewCascadeImpactCDTO impact = new CapNewCascadeImpactCDTO();
        impact.setSourceStep(savedStep);
        impact.setSourceStepLabel(CapNewStep.resolve(savedStep).getLabel());

        List<Integer> affected = new ArrayList<>();
        List<String> affectedLabels = new ArrayList<>();

        for (int stepNum = savedStep + 1; stepNum <= 8; stepNum++) {
            if (!payload.containsKey("step" + stepNum)) {
                continue;
            }
            Object raw = payload.get("step" + stepNum);
            if (!(raw instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> stepData = new LinkedHashMap<>((Map<String, Object>) raw);
            try {
                CapNewStep step = CapNewStep.resolve(stepNum);
                CapNewStepValidationCDTO stepValidation = validateAndEnrich(step, payload, stepData);
                payload.put("step" + stepNum, stepData);
                affected.add(stepNum);
                affectedLabels.add(step.getLabel());
                warnings.addAll(stepValidation.getWarnings());
                if (!stepValidation.isValid()) {
                    warnings.add("STEP " + stepNum + " 재산정 검증 오류: "
                            + String.join(", ", stepValidation.getErrors()));
                }
            } catch (CapNewBizException ex) {
                warnings.add("STEP " + stepNum + " 재산정 중단: " + ex.getMessage());
                break;
            }
        }

        Map<String, String> afterSnapshot = CapNewStepSnapshot.extract(payload);
        List<CapNewCascadeImpactCDTO.ChangeItem> changes = CapNewStepSnapshot.diff(beforeSnapshot, afterSnapshot);

        impact.setRecalculated(!affected.isEmpty());
        impact.setAffectedSteps(affected);
        impact.setAffectedStepLabels(affectedLabels);
        impact.setChanges(changes);

        if (!affected.isEmpty()) {
            if (!changes.isEmpty()) {
                String primary = CapNewStepSnapshot.formatPrimaryChange(savedStep, changes);
                impact.setMessage(primary + " — 하위 " + affected.size() + "개 단계 자동 재산정");
                impact.setSummary("STEP " + savedStep + " 저장 → STEP "
                        + affected.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("")
                        + " 재산정 · 변경 지표 " + changes.size() + "건");
            } else {
                impact.setMessage("하위 " + affected.size() + "개 단계가 재산정되었습니다. (지표 변화 없음)");
                impact.setSummary(impact.getMessage());
            }
        } else {
            impact.setSummary("하위 단계 재산정 대상 없음");
        }
        return impact;
    }

    private void requirePriorSteps(Map<String, Object> payload, int throughStep) {
        for (int i = 1; i <= throughStep; i++) {
            if (!payload.containsKey("step" + i)) {
                throw new CapNewBizException("STEP " + i + " 데이터가 없습니다. 이전 단계를 먼저 저장하세요.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int resolveTotalUsers(Map<String, Object> payload) {
        Object step2 = payload.get("step2");
        if (step2 instanceof Map<?, ?> map) {
            Object totalUsers = map.get("totalUsers");
            if (totalUsers instanceof Number number) {
                return number.intValue();
            }
        }
        return 36000;
    }

    @SuppressWarnings("unchecked")
    private void applyMetadataFromStep1(CapNewScenarioRow row, Map<String, Object> payload) {
        Object step1 = payload.get("step1");
        if (!(step1 instanceof Map<?, ?> map)) {
            return;
        }
        if (map.get("projectId") != null) {
            row.setProjectId(String.valueOf(map.get("projectId")));
        }
        if (map.get("projectName") != null) {
            row.setProjectName(String.valueOf(map.get("projectName")));
        }
        if (map.get("scenarioName") != null) {
            row.setScenarioName(String.valueOf(map.get("scenarioName")));
        }
        if (map.get("targetEnv") != null) {
            row.setTargetEnv(String.valueOf(map.get("targetEnv")));
        }
        if (map.get("baseDate") != null) {
            row.setBaseDate(resolveBaseDate(String.valueOf(map.get("baseDate"))));
        }
        if (map.get("versionNo") != null) {
            row.setVersionNo(String.valueOf(map.get("versionNo")));
        }
        if (map.get("author") != null) {
            row.setAuthor(String.valueOf(map.get("author")));
        }
        if (map.get("description") != null) {
            row.setDescription(String.valueOf(map.get("description")));
        }
        if (map.get("purpose") != null) {
            row.setPurpose(String.valueOf(map.get("purpose")));
        }
    }

    private CapNewScenarioRow requireScenario(String scenarioId) {
        CapNewScenarioRow row = scenarioDao.findById(scenarioId);
        if (row == null) {
            throw new CapNewBizException("시나리오를 찾을 수 없습니다: " + scenarioId);
        }
        return row;
    }

    private CapNewScenarioSummaryCDTO toSummary(CapNewScenarioRow row) {
        CapNewScenarioSummaryCDTO dto = new CapNewScenarioSummaryCDTO();
        dto.setScenarioId(row.getScenarioId());
        dto.setProjectId(row.getProjectId());
        dto.setProjectName(row.getProjectName());
        dto.setScenarioName(row.getScenarioName());
        dto.setTargetEnv(row.getTargetEnv());
        dto.setStatus(row.getStatus());
        dto.setCurrentStep(row.getCurrentStep());
        dto.setUpdatedAt(row.getUpdatedAt());
        return dto;
    }

    private CapNewScenarioCDTO toDetail(CapNewScenarioRow row) {
        CapNewScenarioCDTO dto = new CapNewScenarioCDTO();
        dto.setScenarioId(row.getScenarioId());
        dto.setProjectId(row.getProjectId());
        dto.setProjectName(row.getProjectName());
        dto.setScenarioName(row.getScenarioName());
        dto.setTargetEnv(row.getTargetEnv());
        dto.setBaseDate(row.getBaseDate());
        dto.setVersionNo(row.getVersionNo());
        dto.setAuthor(row.getAuthor());
        dto.setDescription(row.getDescription());
        dto.setPurpose(row.getPurpose());
        dto.setStatus(row.getStatus());
        dto.setCurrentStep(row.getCurrentStep());
        dto.setStepPayload(readPayload(row.getStepPayload()));
        dto.setStepTrack(buildStepTrack(dto.getStepPayload()));
        dto.setCreatedAt(row.getCreatedAt());
        dto.setUpdatedAt(row.getUpdatedAt());
        return dto;
    }

    private CapNewStepValidationCDTO validateSavedStep(CapNewStep step, Map<String, Object> stepData) {
        Map<String, Object> snapshot = new LinkedHashMap<>(stepData);
        return switch (step) {
            case BASIC -> stepRule.validateStep1(snapshot);
            case USER_SESSION -> stepRule.validateStep2(snapshot);
            case TPS -> stepRule.validateStep3(snapshot);
            case VM -> stepRule.validateStep4(snapshot);
            case AP_DR -> stepRule.validateStep5(snapshot);
            case WAS -> stepRule.validateStep6(snapshot);
            case DB_POOL -> stepRule.validateStep7(snapshot);
            case SUMMARY -> stepRule.validateStep8(snapshot);
        };
    }

    @SuppressWarnings("unchecked")
    private List<CapNewStepTrackStatusCDTO> buildStepTrack(Map<String, Object> payload) {
        List<CapNewStepTrackStatusCDTO> tracks = new ArrayList<>();
        int highestPresent = 0;
        for (int i = 8; i >= 1; i--) {
            if (hasStepData(payload, i)) {
                highestPresent = i;
                break;
            }
        }

        for (CapNewStep step : CapNewStep.values()) {
            int stepNum = step.getNumber();
            CapNewStepTrackStatusCDTO track = new CapNewStepTrackStatusCDTO();
            track.setStep(stepNum);
            track.setLabel(step.getLabel());

            if (!hasStepData(payload, stepNum)) {
                if (stepNum <= highestPresent) {
                    applyTrackState(track, "error", "×", "미입력");
                    track.addIssue("저장된 데이터가 없습니다.");
                } else {
                    applyTrackState(track, "pending", "○", "대기");
                }
                tracks.add(track);
                continue;
            }

            Map<String, Object> stepData = (Map<String, Object>) payload.get("step" + stepNum);
            CapNewStepValidationCDTO validation = validateSavedStep(step, stepData);
            if (!validation.isValid()) {
                applyTrackState(track, "error", "×", "검증 오류");
                validation.getErrors().forEach(track::addIssue);
            } else if (!validation.getWarnings().isEmpty() || hasRiskSignals(stepNum, stepData)) {
                applyTrackState(track, "warn", "!", "주의");
                validation.getWarnings().forEach(track::addIssue);
                collectRiskSignals(stepNum, stepData).stream()
                        .filter(msg -> validation.getWarnings().stream().noneMatch(w -> w.contains(msg)))
                        .forEach(track::addIssue);
            } else {
                applyTrackState(track, "done", "✓", "완료");
            }
            tracks.add(track);
        }
        return tracks;
    }

    private boolean hasStepData(Map<String, Object> payload, int stepNum) {
        Object raw = payload.get("step" + stepNum);
        return raw instanceof Map<?, ?> map && !map.isEmpty();
    }

    private void applyTrackState(CapNewStepTrackStatusCDTO track, String state, String symbol, String hint) {
        track.setState(state);
        track.setSymbol(symbol);
        track.setHint(hint);
    }

    @SuppressWarnings("unchecked")
    private boolean hasRiskSignals(int stepNum, Map<String, Object> stepData) {
        return !collectRiskSignals(stepNum, stepData).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<String> collectRiskSignals(int stepNum, Map<String, Object> stepData) {
        List<String> signals = new ArrayList<>();
        switch (stepNum) {
            case 5 -> {
                List<?> rows = stepData.get("scenarioResults") instanceof List<?> list ? list : List.of();
                for (Object row : rows) {
                    if (row instanceof Map<?, ?> map && isRiskStatus(text(map.get("judgment")))) {
                        signals.add(text(map.get("label")) + ": " + text(map.get("judgment")));
                    }
                }
            }
            case 6 -> addRiskSignal(signals, text(stepData.get("wasStatus")), text(stepData.get("wasStatusMessage")));
            case 7 -> {
                addRiskSignal(signals, text(stepData.get("dbStatus")), text(stepData.get("dbStatusMessage")));
                if (boolValue(stepData.get("warPoolEnabled"), false)) {
                    addRiskSignal(signals, text(stepData.get("warPoolStatus")), text(stepData.get("warPoolStatusMessage")));
                }
            }
            case 8 -> {
                Object headline = stepData.get("headline");
                if (headline instanceof Map<?, ?> map) {
                    addRiskSignal(signals, text(map.get("overallJudgment")), "종합 판정");
                }
            }
            default -> { }
        }
        return signals;
    }

    private void addRiskSignal(List<String> signals, String status, String message) {
        if (!isRiskStatus(status)) {
            return;
        }
        String detail = StringUtils.hasText(message) ? message : status;
        if (!signals.contains(detail)) {
            signals.add(detail);
        }
    }

    private boolean isRiskStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "WARN".equals(normalized) || "WARNING".equals(normalized) || "CRITICAL".equals(normalized);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean boolValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return defaultValue;
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

    private String resolveBaseDate(String baseDate) {
        if (!StringUtils.hasText(baseDate)) {
            return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return baseDate;
    }
}
