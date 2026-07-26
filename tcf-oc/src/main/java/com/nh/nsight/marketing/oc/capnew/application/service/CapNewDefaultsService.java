package com.nh.nsight.marketing.oc.capnew.application.service;

import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewDefaultsCDTO;
import com.nh.nsight.marketing.oc.capnew.application.rule.CapNewStepRule;
import com.nh.nsight.marketing.oc.support.VmProfile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CapNewDefaultsService {

    private final CapNewStepRule stepRule;

    public CapNewDefaultsService(CapNewStepRule stepRule) {
        this.stepRule = stepRule;
    }

    public CapNewDefaultsCDTO defaults() {
        CapNewDefaultsCDTO dto = new CapNewDefaultsCDTO();

        Map<String, Object> step1 = new LinkedHashMap<>();
        step1.put("projectId", "NSIGHT-MP");
        step1.put("projectName", "NSIGHT 마케팅플랫폼");
        step1.put("scenarioName", LocalDate.now().getYear() + " 운영용량 기준안");
        step1.put("targetEnv", "PROD");
        step1.put("baseDate", LocalDate.now().toString());
        step1.put("versionNo", "V1.0");
        step1.put("author", "");
        step1.put("description", "");
        step1.put("purpose", "NEW_BUILD");
        dto.setStep1(step1);

        Map<String, Object> step2 = new LinkedHashMap<>();
        step2.put("calcMode", "BRANCH");
        step2.put("branchCount", 6000);
        step2.put("userPerBranch", 6);
        step2.put("hqUsers", 0);
        step2.put("otherUsers", 0);
        step2.put("sessionMarginRate", 0.30);
        step2.put("sessionTimeoutMin", 60);
        dto.setStep2(stepRule.enrichStep2(step2));

        dto.setTpsPresets(stepRule.defaultTpsScenarios(intValue(dto.getStep2().get("totalUsers"), 36000)));
        dto.setTargetEnvs(List.of("DEV", "STG", "PROD", "DR"));
        dto.setPurposes(List.of("NEW_BUILD", "SCALE_OUT", "CONFIG_CHECK", "DR_VALIDATION"));
        dto.setVmProfiles(vmProfiles());
        dto.setBusinessTypes(businessTypes());
        dto.setCenterModes(centerModes());
        dto.setStep4(defaultStep4());
        dto.setStep5(defaultStep5());
        dto.setStep6(defaultStep6());
        dto.setStep7(defaultStep7());
        dto.setWarPresets(defaultWarPresets());
        return dto;
    }

    private Map<String, Object> defaultStep4() {
        Map<String, Object> step4 = new LinkedHashMap<>();
        step4.put("businessTypeCode", "SINGLE_VIEW");
        step4.put("vmProfileCode", "16CORE-128GB");
        step4.put("tpmcPerTps", 3000);
        step4.put("tpsPerCore", 36);
        step4.put("cpuTargetUtilization", 0.70);
        step4.put("perfSafetyFactor", 1.20);
        step4.put("virtualizationFactor", 0.90);
        step4.put("opsEfficiencyFactor", 0.85);
        step4.put("applyCorrectionFactors", true);
        return step4;
    }

    private Map<String, Object> defaultStep5() {
        Map<String, Object> step5 = new LinkedHashMap<>();
        step5.put("centerMode", "ACTIVE_ACTIVE");
        step5.put("trafficSplit", "50:50");
        step5.put("drSingleCenterFullLoad", true);
        step5.put("apMarginPerCenter", 1);
        step5.put("minApPerCenter", 2);
        return step5;
    }

    private Map<String, Object> defaultStep6() {
        Map<String, Object> step6 = new LinkedHashMap<>();
        step6.put("baselineScenarioCode", "DESIGN_PEAK");
        step6.put("avgThreadHoldSec", 1.2);
        step6.put("threadMarginRate", 1.2);
        step6.put("maxThreadMarginRate", 1.3);
        return step6;
    }

    private Map<String, Object> defaultStep7() {
        Map<String, Object> step7 = new LinkedHashMap<>();
        step7.put("apType", "SINGLE_VIEW");
        step7.put("avgDbConnectionHoldSec", 0.20);
        step7.put("dbTransactionUsageRatio", 1.0);
        step7.put("poolSafetyFactor", 1.3);
        step7.put("threadDbUsageRatio", 0.30);
        step7.put("minPoolPerVm", 30);
        step7.put("minPoolPerWar", 15);
        step7.put("dbSessionLimit", 800);
        step7.put("warPoolEnabled", true);
        step7.put("warAllocations", defaultWarAllocations());
        return step7;
    }

    private List<Map<String, Object>> defaultWarAllocations() {
        List<Map<String, Object>> wars = new ArrayList<>();
        wars.add(warPreset("SV", "SV", 40, true));
        wars.add(warPreset("IC", "IC", 25, true));
        wars.add(warPreset("MG", "MG", 20, true));
        wars.add(warPreset("OTHER", "기타", 15, true));
        return wars;
    }

    private List<Map<String, Object>> defaultWarPresets() {
        List<Map<String, Object>> wars = new ArrayList<>();
        wars.add(warPreset("SV", "SV (서비스)", 40, true));
        wars.add(warPreset("IC", "IC (통합고객)", 25, true));
        wars.add(warPreset("MG", "MG (마케팅)", 20, true));
        wars.add(warPreset("EB", "EB (전자뱅킹)", 0, false));
        wars.add(warPreset("PC", "PC (상품)", 0, false));
        wars.add(warPreset("OTHER", "기타", 15, true));
        return wars;
    }

    private Map<String, Object> warPreset(String code, String label, int weight, boolean enabled) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("warCode", code);
        row.put("label", label);
        row.put("weightPercent", weight);
        row.put("enabled", enabled);
        return row;
    }

    private List<Map<String, Object>> centerModes() {
        List<Map<String, Object>> modes = new ArrayList<>();
        modes.add(centerMode("ACTIVE_ACTIVE", "2센터 Active-Active"));
        modes.add(centerMode("DR_STANDBY", "운영센터 + DR 대기"));
        modes.add(centerMode("SINGLE", "단일센터"));
        return modes;
    }

    private Map<String, Object> centerMode(String code, String label) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", code);
        row.put("label", label);
        return row;
    }

    private List<Map<String, Object>> vmProfiles() {
        List<Map<String, Object>> profiles = new ArrayList<>();
        addVm(profiles, VmProfile.CORE4_32);
        addVm(profiles, VmProfile.CORE8_64);
        addVm(profiles, VmProfile.CORE16_128);
        addVm(profiles, VmProfile.CORE32_256);
        return profiles;
    }

    private void addVm(List<Map<String, Object>> profiles, VmProfile profile) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", profile.getId());
        row.put("label", profile.getId());
        row.put("cores", profile.getCores());
        row.put("memoryGb", profile.getMemoryGb());
        row.put("guideNominalTps", profile.getGuideNominalTps());
        profiles.add(row);
    }

    private List<Map<String, Object>> businessTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        types.add(businessType("CACHE", "단순 캐시 조회", 1500, 71));
        types.add(businessType("SIMPLE", "일반 단건 조회", 2000, 53));
        types.add(businessType("SINGLE_VIEW", "SingleView 조회", 3000, 36));
        types.add(businessType("COMPLEX", "복합 고객 조회", 4000, 27));
        types.add(businessType("TXN", "변경·대외 연계 거래", 5000, 21));
        return types;
    }

    private Map<String, Object> businessType(String code, String label, int tpmc, int tpsPerCore) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", code);
        row.put("label", label);
        row.put("tpmcPerTps", tpmc);
        row.put("tpsPerCore", tpsPerCore);
        return row;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}
