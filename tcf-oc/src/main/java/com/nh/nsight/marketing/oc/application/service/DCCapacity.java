package com.nh.nsight.marketing.oc.application.service;

import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation;
import com.nh.nsight.marketing.oc.support.VmProfile;
import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationResultCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityScenarioResultCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.DbPoolResultCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.WasThreadResultCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationDDTO;
import com.nh.nsight.marketing.oc.support.CapacityCalcStep;
import com.nh.nsight.marketing.oc.support.NsightDbPoolDerivation;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 용량산정 도메인 — 설계서 TPS · AP · WAS Thread · DB Pool 산출.
 */
@Component
public class DCCapacity {

    private static final String DC = "DCCapacity";

    public CapacityCalculationResultCDTO calculate(CapacityCalculationDDTO input) {
        return calculate(input, CapacityCalcStep.ALL);
    }

    public CapacityCalculationResultCDTO calculate(CapacityCalculationDDTO input, CapacityCalcStep step) {
        System.out.println("★★★★★ [" + DC + "] calculate START step=" + step.getCode()
                + " project=" + input.projectName() + " users=" + input.totalUsers());
        String scenarioId = "CAP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        VmProfile profile = profileById(input.vmProfileId());
        int vmTpsMax = profile.getCores() * NsightCapacityDerivation.REF_TPS_PER_CORE_MAX;

        List<CapacityScenarioResultCDTO> rows = new ArrayList<>();
        Map<String, Integer> risk = new LinkedHashMap<>();
        risk.put("normal", 0);
        risk.put("warning", 0);
        risk.put("critical", 0);

        for (double rate : input.concurrentRequestRates()) {
            int percent = (int) Math.round(rate * 100);
            int concurrentUsers = NsightCapacityDerivation.expectedActualRequestFromPercent(input.totalUsers(),
                    percent);
            for (int timeoutSec : input.targetResponseTimes()) {
                int targetTps = NsightCapacityDerivation.peakTpsFromActualRequestUsers(concurrentUsers,
                        timeoutSec * 1000);
                long requiredTpmc = (long) targetTps * input.tpmcPerTps();
                int requiredCore = (int) Math.ceil((double) targetTps / Math.max(1, input.tpsPerCore()));

                int requiredAp = 0;
                int recommendedAp = 0;
                int apForWas = 1;
                if (step.includesAp()) {
                    requiredAp = (int) Math.ceil((double) targetTps / Math.max(1, input.vmTpsAtBase()));
                    recommendedAp = requiredAp;
                    int deploymentAp = input.activeActive() ? requiredAp * 2 : requiredAp;
                    apForWas = Math.max(1, deploymentAp);
                }

                WasThreadResultCDTO was = null;
                if (step.includesWas()) {
                    was = buildWasThread(
                            targetTps, apForWas, input.avgThreadHoldSec(),
                            input.threadMarginRate(), input.maxThreadMarginRate(), profile);
                }

                DbPoolResultCDTO db = null;
                if (step.includesDb()) {
                    int threadsPerVm = was != null ? was.getThreadsPerVm() : 1;
                    db = buildDbPool(targetTps, apForWas, threadsPerVm, input);
                }

                String tpsStatus = classifyTpsForStep(step, targetTps, input.vmTpsAtBase(), vmTpsMax,
                        requiredAp, recommendedAp, input.drValidation(), input.activeActive(), db,
                        input.validateDbPool());
                bumpRisk(risk, tpsStatus);
                if (was != null && !"NORMAL".equals(was.getStatus())) {
                    bumpRisk(risk, was.getStatus());
                }
                if (db != null && !"NORMAL".equals(db.getStatus())) {
                    bumpRisk(risk, db.getStatus());
                }

                CapacityScenarioResultCDTO row = new CapacityScenarioResultCDTO();
                row.setConcurrentRate(rate);
                row.setResponseTimeSec(timeoutSec);
                row.setConcurrentRequestUsers(concurrentUsers);
                row.setTargetTps(targetTps);
                row.setRequiredTpmc(requiredTpmc);
                row.setRequiredCore(requiredCore);
                row.setRequiredApCount(requiredAp);
                row.setRecommendedApCount(recommendedAp);
                row.setDeploymentApCount(step.includesAp() ? apForWas : 0);
                int availableTps = requiredAp > 0
                        ? requiredAp * input.vmTpsAtBase()
                        : requiredCore * input.tpsPerCore();
                row.setAvailableTps(availableTps);
                row.setVmTpsAtBase(input.vmTpsAtBase());
                row.setVmProfileId(input.vmProfileId());
                row.setWasThread(was);
                row.setDbPool(db);
                String rowStatus = worstRowStatus(tpsStatus, was != null ? was.getStatus() : null,
                        db != null ? db.getStatus() : null);
                row.setTpsStatus(rowStatus);
                row.setTpsStatusReason(buildRowStatusReason(step, rowStatus, tpsStatus, targetTps, input,
                        requiredAp, recommendedAp, was, db));
                rows.add(row);
            }
        }

        CapacityCalculationResultCDTO result = new CapacityCalculationResultCDTO();
        result.setScenarioId(scenarioId);
        result.setScenarioLabel(label(input));
        result.setTotalUserCount(input.totalUsers());
        result.setDesignedSessionCount(input.designedSessions());
        result.setVmProfileId(input.vmProfileId());
        result.setVmCores(input.vmCores());
        result.setVmMemoryGb(input.vmMemoryGb());
        result.setVmTpsAtBase(input.vmTpsAtBase());
        result.setSummaryFormula(buildSummaryForStep(input, step));
        result.setRiskSummary(risk);
        result.setResults(rows);
        result.setCalculatedStep(step.getCode());
        result.setCalculatedStepLabel(step.getLabel());
        System.out.println("★★★★★ [" + DC + "] calculate END step=" + step.getCode()
                + " scenarioId=" + scenarioId + " rows=" + rows.size());
        return result;
    }

    public WasThreadResultCDTO calculateWasThreadOnly(
            int targetTps,
            int apCount,
            double avgHoldSec,
            double threadMargin,
            double maxMargin,
            VmProfile profile) {
        return buildWasThread(targetTps, Math.max(1, apCount), avgHoldSec, threadMargin, maxMargin, profile);
    }

    private WasThreadResultCDTO buildWasThread(
            int targetTps,
            int apCount,
            double avgHoldSec,
            double threadMargin,
            double maxMargin,
            VmProfile profile) {
        int totalThreads = (int) Math.ceil(targetTps * avgHoldSec * threadMargin);
        int threadsPerVm = (int) Math.ceil((double) totalThreads / apCount);
        int maxThreads = (int) Math.ceil(threadsPerVm * maxMargin);
        int minSpare = (int) Math.ceil(maxThreads * 0.25);
        int acceptCount = (int) Math.ceil(maxThreads * 0.5);

        var spec = profile.getTomcatHikariSpec();
        int profileMax = spec.tomcatMaxThreadsMax();
        String status;
        String message;
        double ratio = profileMax > 0 ? (double) threadsPerVm / profileMax : 0;
        if (threadsPerVm > profileMax * 0.85) {
            status = "CRITICAL";
            message = "CAP-WAS-003: VM당 필요 Thread가 maxThreads 권장(85%) 초과";
        } else if (threadsPerVm > profileMax * 0.70) {
            status = "WARN";
            message = "CAP-WAS-002: Thread 사용률 높음 — AP 증설 또는 점유시간 조정 검토";
        } else if (maxThreads > profileMax * 1.10) {
            status = "WARN";
            message = "CAP-WAS-004: maxThreads가 VM 프로파일 상한 대비 과다";
        } else {
            status = "NORMAL";
            message = "CAP-WAS-001: Thread 여유 범위 내";
        }

        WasThreadResultCDTO dto = new WasThreadResultCDTO();
        dto.setTotalCalculatedThreads(totalThreads);
        dto.setThreadsPerVm(threadsPerVm);
        dto.setRecommendedMaxThreads(maxThreads);
        dto.setMinSpareThreads(minSpare);
        dto.setAcceptCount(acceptCount);
        dto.setStatus(status);
        dto.setStatusMessage(message + " (프로파일 " + profile.getId() + " max≈" + profileMax + ", ratio="
                + String.format("%.2f", ratio) + ")");
        return dto;
    }

    private DbPoolResultCDTO buildDbPool(
            int targetTps,
            int apCount,
            int threadsPerVm,
            CapacityCalculationDDTO input) {
        double apTps = (double) targetTps / Math.max(1, apCount);
        var sizing = NsightDbPoolDerivation.recommend(new NsightDbPoolDerivation.Input(
                apTps,
                threadsPerVm,
                input.avgDbConnectionHoldSec(),
                input.dbTransactionUsageRatio(),
                input.poolSafetyFactor(),
                input.threadDbUsageRatio(),
                input.minPoolPerVm(),
                input.profilePoolCap()));

        int poolPerVm = sizing.recommendedPool();
        long totalSessions = (long) apCount * poolPerVm;
        double ratio = sizing.threadPoolRatio();

        String status = "NORMAL";
        String message = sizing.formulaSummary();
        if (sizing.theoreticalPool() > sizing.ceilingPool()) {
            status = "WARN";
            message = "TPS 산출(" + sizing.theoreticalPool() + ") > Thread 상한(" + sizing.ceilingPool()
                    + ") — SQL/점유시간 개선 또는 Thread 증설 검토 · " + message;
        }
        if (ratio > 12) {
            status = "CRITICAL";
            message = "Thread/Pool " + String.format("%.1f", ratio) + ":1 — Pool 또는 SQL 검토 · " + message;
        } else if (ratio > 8) {
            status = "WARN";
            message = "Thread/Pool " + String.format("%.1f", ratio) + ":1 — 모니터링 · " + message;
        }
        boolean minPoolFloorApplied = poolPerVm >= input.minPoolPerVm()
                && sizing.sizedPool() < input.minPoolPerVm();
        if (minPoolFloorApplied && poolPerVm > sizing.ceilingPool()) {
            status = "WARN";
            message = "CAP-050: 운영 최소(" + input.minPoolPerVm() + ")가 ③ Thread 상한("
                    + sizing.ceilingPool() + ")보다 큼 — ⑤ 용량권장=" + sizing.sizedPool()
                    + ", ④ 배포=" + poolPerVm + " (저부하·운영하한) · " + message;
        }
        if (!minPoolFloorApplied && sizing.ceilingPool() > 0) {
            double usageVsCeiling = (double) poolPerVm / sizing.ceilingPool();
            if (usageVsCeiling > 0.85) {
                status = "CRITICAL";
                message = "CAP-DB: Pool이 Thread 상한의 85% 초과 · " + message;
            } else if (usageVsCeiling > 0.70) {
                status = "WARN";
                message = "CAP-DB: Pool이 Thread 상한의 70% 초과 — SQL·Pool 조정 검토 · " + message;
            }
        }
        if (input.validateDbPool() && totalSessions > input.dbSessionLimit() * 0.8) {
            status = totalSessions > input.dbSessionLimit() ? "CRITICAL" : "WARN";
            message = "CAP-DB-001: DB Session " + totalSessions + " / 한도 " + input.dbSessionLimit()
                    + " · " + message;
        }

        DbPoolResultCDTO dto = new DbPoolResultCDTO();
        dto.setApTpsPerVm(sizing.apTpsRounded());
        dto.setPoolTheoretical(sizing.theoreticalPool());
        dto.setPoolCeiling(sizing.ceilingPool());
        dto.setPoolSized(sizing.sizedPool());
        dto.setPoolPerVm(poolPerVm);
        dto.setApCountForPool(apCount);
        dto.setMinPoolFloorApplied(minPoolFloorApplied);
        dto.setTotalDbSessions(totalSessions);
        dto.setThreadPoolRatio(ratio);
        dto.setPoolFormula(sizing.formulaSummary());
        dto.setStatus(status);
        dto.setStatusMessage(message);
        return dto;
    }

    private String classifyTpsForStep(
            CapacityCalcStep step,
            int targetTps,
            int vmTpsBase,
            int vmTpsMax,
            int requiredAp,
            int recommendedAp,
            boolean drValidation,
            boolean activeActive,
            DbPoolResultCDTO db,
            boolean validateDbPool) {
        if (step.includesDb() && db != null && validateDbPool && "CRITICAL".equals(db.getStatus())) {
            return "CRITICAL";
        }
        if (step.includesAp() && requiredAp > 0) {
            if (targetTps > vmTpsMax * requiredAp) {
                return "CRITICAL";
            }
            int apTpsPerVm = (int) Math.ceil((double) targetTps / requiredAp);
            if (apTpsPerVm > vmTpsMax) {
                return "CRITICAL";
            }
            if (apTpsPerVm > vmTpsBase) {
                return "WARN";
            }
            if (drValidation && activeActive) {
                if (requiredAp < 2 && targetTps > vmTpsBase) {
                    return "WARN";
                }
            }
            return "NORMAL";
        }
        if (targetTps > vmTpsBase * 0.8) {
            return "WARN";
        }
        return "NORMAL";
    }

    private String buildTpsReasonForStep(
            CapacityCalcStep step,
            String status,
            int targetTps,
            CapacityCalculationDDTO input,
            int requiredAp,
            int recommendedAp,
            DbPoolResultCDTO db) {
        if ("CRITICAL".equals(status) && step.includesDb() && db != null
                && db.getStatusMessage() != null && db.getStatusMessage().contains("CAP-DB")) {
            return db.getStatusMessage();
        }
        int apTpsPerVm = requiredAp > 0
                ? (int) Math.ceil((double) targetTps / requiredAp)
                : targetTps;
        return switch (status) {
            case "CRITICAL" -> step.includesAp()
                    ? "클러스터 TPS " + targetTps + " > VM최대(" + input.vmCores() + "C×40)×AP "
                    + requiredAp + " 또는 VM당 AP TPS " + apTpsPerVm + " > VM 최대 TPS"
                    : "목표 TPS " + targetTps + " — VM TPS(기준) " + input.vmTpsAtBase() + " 초과 검토";
            case "WARN" -> step.includesAp()
                    ? buildApWarnReason(targetTps, apTpsPerVm, input.vmTpsAtBase(), requiredAp, recommendedAp,
                    input.drValidation(), input.activeActive())
                    : "TPS 경계 — 목표 " + targetTps + " > VM TPS(기준) " + input.vmTpsAtBase() + "×80% (CAP-020)";
            default -> step.getCode().equals("020")
                    ? "CAP-020 TPS 산정 완료 — AP·WAS·DB 단계 미포함"
                    : step.includesAp()
                    ? "VM당 AP TPS " + apTpsPerVm + " ≤ VM TPS(기준) " + input.vmTpsAtBase() + " — AP·DR 범위 내"
                    : "처리량 범위 내";
        };
    }

    private static String worstRowStatus(String tpsStatus, String wasStatus, String dbStatus) {
        if ("CRITICAL".equals(dbStatus) || "CRITICAL".equals(wasStatus) || "CRITICAL".equals(tpsStatus)) {
            return "CRITICAL";
        }
        if ("WARN".equals(dbStatus) || "WARN".equals(wasStatus) || "WARN".equals(tpsStatus)) {
            return "WARN";
        }
        return "NORMAL";
    }

    private String buildRowStatusReason(
            CapacityCalcStep step,
            String rowStatus,
            String tpsStatus,
            int targetTps,
            CapacityCalculationDDTO input,
            int requiredAp,
            int recommendedAp,
            WasThreadResultCDTO was,
            DbPoolResultCDTO db) {
        if ("CRITICAL".equals(rowStatus) || "WARN".equals(rowStatus)) {
            if (db != null && rowStatus.equals(db.getStatus()) && db.getStatusMessage() != null) {
                return db.getStatusMessage();
            }
            if (was != null && rowStatus.equals(was.getStatus()) && was.getStatusMessage() != null) {
                return was.getStatusMessage();
            }
        }
        return buildTpsReasonForStep(step, tpsStatus, targetTps, input, requiredAp, recommendedAp, db);
    }

    private String buildApWarnReason(
            int targetTps,
            int apTpsPerVm,
            int vmTpsBase,
            int requiredAp,
            int recommendedAp,
            boolean drValidation,
            boolean activeActive) {
        if (drValidation && activeActive && requiredAp < 2) {
            return "CAP-DR-001: A-A 시 센터당 필요 AP " + requiredAp
                    + "대 — 2대 이상 검토 (목표 TPS " + targetTps + ")";
        }
        return "CAP-AP: VM당 AP TPS " + apTpsPerVm + " > VM TPS(기준) " + vmTpsBase
                + " (목표 " + targetTps + " ÷ 필요 AP " + requiredAp + ")";
    }

    private void bumpRisk(Map<String, Integer> risk, String status) {
        String key = switch (status) {
            case "WARN", "WARNING" -> "warning";
            case "CRITICAL" -> "critical";
            default -> "normal";
        };
        risk.merge(key, 1, Integer::sum);
    }

    private String label(CapacityCalculationDDTO input) {
        return input.totalUsers() + "명 / " + input.vmProfileId()
                + " / 세션 " + input.sessionTimeoutMin() + "분";
    }

    private String buildSummaryForStep(CapacityCalculationDDTO input, CapacityCalcStep step) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(step.getLabel()).append("] ");
        sb.append("실요청자=전체×% · 목표TPS=실요청자÷Timeout · TPMC=TPS×").append(input.tpmcPerTps());
        if (step.includesAp()) {
            sb.append(" · VM TPS=").append(input.vmCores()).append("×").append(input.tpsPerCore())
                    .append("=").append(input.vmTpsAtBase());
            if (input.activeActive()) {
                sb.append(" · A-A=센터당 필요AP×2(총배포)");
            }
        }
        if (step.includesWas()) {
            sb.append(" · WAS Thread=AP TPS×점유×여유");
        }
        if (step.includesDb()) {
            sb.append(" · DB Pool=⑤min(②,③) · ④max(운영최소").append(input.minPoolPerVm())
                    .append(",⑤) · ②=AP TPS×").append(input.avgDbConnectionHoldSec()).append("s×DB비율×")
                    .append(input.poolSafetyFactor()).append(" · ③=Thread×")
                    .append((int) (input.threadDbUsageRatio() * 100)).append("%");
        }
        return sb.toString();
    }

    private static VmProfile profileById(String id) {
        String normalized = VmProfile.normalizeProfileId(id);
        if (normalized != null) {
            for (VmProfile profile : VmProfile.values()) {
                if (profile.getId().equalsIgnoreCase(normalized)) {
                    return profile;
                }
            }
        }
        return VmProfile.CORE8_64;
    }
}
