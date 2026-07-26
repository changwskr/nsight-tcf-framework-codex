package com.nh.nsight.marketing.oc.capnew.application.service;

import com.nh.nsight.marketing.oc.application.dto.env.CapacityDesignView;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerRequest;
import com.nh.nsight.marketing.oc.application.service.env.CapacityDesignService;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewEnvHandoffCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioCDTO;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import com.nh.nsight.marketing.oc.capnew.support.CapNewScenarioStatus;
import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * NEW 용량산정 → ENV-002 handoff (기존 ENV API·서비스 참조만).
 */
@Service
public class CapNewEnvBridgeService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CapNewWizardService wizardService;
    private final CapacityDesignService capacityDesignService;

    public CapNewEnvBridgeService(
            CapNewWizardService wizardService,
            CapacityDesignService capacityDesignService) {
        this.wizardService = wizardService;
        this.capacityDesignService = capacityDesignService;
    }

    public CapNewEnvHandoffCDTO buildHandoff(String scenarioId) {
        CapNewScenarioCDTO scenario = wizardService.getScenario(scenarioId);
        validateHandoffable(scenario);

        CapacityPlannerRequest request = toPlannerRequest(scenario);
        CapacityDesignView view = capacityDesignService.analyze(request);

        CapNewEnvHandoffCDTO handoff = new CapNewEnvHandoffCDTO();
        handoff.setCapNewScenarioId(scenario.getScenarioId());
        handoff.setCapNewScenarioName(scenario.getScenarioName());
        handoff.setProjectName(scenario.getProjectName());
        handoff.setTargetEnv(scenario.getTargetEnv());
        handoff.setEnvPageUrl("/oc/env-002.html");
        handoff.setHandoffAt(TS.format(LocalDateTime.now()));
        handoff.setNote("NEW 용량산정 [" + scenario.getScenarioName() + "] → ENV-002 환경 점검");
        handoff.setCapacityRequest(request);
        handoff.setCapacityView(view);
        return handoff;
    }

    private void validateHandoffable(CapNewScenarioCDTO scenario) {
        String status = scenario.getStatus();
        if (!CapNewScenarioStatus.COMPLETED.name().equals(status)
                && !CapNewScenarioStatus.APPROVED.name().equals(status)) {
            throw new CapNewBizException("COMPLETED 또는 APPROVED 상태만 ENV 연동할 수 있습니다.");
        }
        Map<String, Object> payload = scenario.getStepPayload();
        if (payload == null || !payload.containsKey("step8")) {
            throw new CapNewBizException("STEP 8 종합 결과가 없습니다. Wizard를 완료하세요.");
        }
    }

    @SuppressWarnings("unchecked")
    private CapacityPlannerRequest toPlannerRequest(CapNewScenarioCDTO scenario) {
        Map<String, Object> payload = scenario.getStepPayload() == null ? Map.of() : scenario.getStepPayload();
        Map<String, Object> step1 = map(payload.get("step1"));
        Map<String, Object> step2 = map(payload.get("step2"));
        Map<String, Object> step3 = map(payload.get("step3"));
        Map<String, Object> step4 = map(payload.get("step4"));
        Map<String, Object> step5 = map(payload.get("step5"));
        Map<String, Object> step7 = map(payload.get("step7"));

        String scenarioName = text(step1.get("scenarioName"), scenario.getScenarioName());
        int branchCount = intVal(step2.get("branchCount"), 3600);
        int usersPerBranch = intVal(step2.get("userPerBranch"), 6);
        int totalUsers = intVal(step2.get("totalUsers"), branchCount * usersPerBranch);
        String vmProfileId = text(step4.get("vmProfileCode"), "16CORE-128GB");
        int tpmcPerTps = intVal(step4.get("tpmcPerTps"), 3000);
        int tpsPerCore = intVal(step4.get("tpsPerCore"), 0);
        boolean manualCoreTps = tpsPerCore > 0;

        var linked = NsightCapacityDerivation.coreTpsFromTpmc(tpmcPerTps);
        int tpsMin = manualCoreTps ? Math.max(1, tpsPerCore - 5) : linked.tpsPerCoreMin();
        int tpsBase = manualCoreTps ? tpsPerCore : linked.tpsPerCoreBase();
        int tpsMax = manualCoreTps ? tpsPerCore + 4 : linked.tpsPerCoreMax();

        List<Integer> percents = extractPercents(step3);
        List<Integer> timeouts = extractTimeouts(step3);
        List<Integer> sessions = extractSessionMinutes(step2);

        boolean activeActive = "ACTIVE_ACTIVE".equalsIgnoreCase(text(step5.get("centerMode"), ""));
        boolean drValidation = boolVal(step5.get("drSingleCenterFullLoad"), true);
        int hikariPool = intVal(step7.get("poolPerVm"), 0);
        int dbSessionLimit = intVal(step7.get("dbSessionLimit"), 500);

        return new CapacityPlannerRequest(
                scenarioName + " (CAP-NEW)",
                branchCount,
                usersPerBranch,
                totalUsers,
                vmProfileId,
                false,
                0,
                0,
                tpsMin,
                tpsBase,
                tpsMax,
                tpmcPerTps,
                manualCoreTps,
                percents,
                timeouts,
                sessions,
                activeActive,
                drValidation,
                true,
                true,
                hikariPool,
                dbSessionLimit);
    }

    @SuppressWarnings("unchecked")
    private List<Integer> extractPercents(Map<String, Object> step3) {
        Set<Integer> percents = new TreeSet<>();
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                if (!boolVal(map.get("enabled"), false)) {
                    continue;
                }
                double rate = doubleVal(map.get("concurrentRate"), 0);
                if (rate > 0 && rate <= 1.0) {
                    rate *= 100.0;
                }
                int pct = (int) Math.round(rate);
                if (pct > 0 && pct <= 100) {
                    percents.add(pct);
                }
            }
        }
        return percents.isEmpty() ? List.of(3, 5, 10, 15) : new ArrayList<>(percents);
    }

    @SuppressWarnings("unchecked")
    private List<Integer> extractTimeouts(Map<String, Object> step3) {
        Set<Integer> timeouts = new TreeSet<>();
        Object raw = step3.get("scenarios");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                if (!boolVal(map.get("enabled"), false)) {
                    continue;
                }
                int sec = intVal(map.get("responseSec"), 0);
                if (sec > 0) {
                    timeouts.add(sec);
                }
            }
        }
        return timeouts.isEmpty() ? List.of(3, 4, 5) : new ArrayList<>(timeouts);
    }

    private List<Integer> extractSessionMinutes(Map<String, Object> step2) {
        int sessionMin = intVal(step2.get("sessionTimeoutMin"), 60);
        Set<Integer> sessions = new LinkedHashSet<>();
        sessions.add(sessionMin);
        if (sessionMin == 60) {
            sessions.add(90);
        }
        return new ArrayList<>(sessions);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private String text(Object value, String fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private int intVal(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double doubleVal(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean boolVal(Object value, boolean fallback) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
