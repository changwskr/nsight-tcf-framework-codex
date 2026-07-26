package com.nh.nsight.marketing.oc.application.rule;

import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationDDTO;
import com.nh.nsight.marketing.oc.support.NsightCapacityDerivation;
import com.nh.nsight.marketing.oc.support.NsightDbPoolDerivation;
import com.nh.nsight.marketing.oc.support.VmProfile;
import com.nh.nsight.marketing.oc.support.OcCapacityBizException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CapacityCDtoConverter {

    private CapacityCDtoConverter() {
    }

    public static CapacityCalculationDDTO toDomain(CapacityCalculationCDTO request) {
        if (request == null) {
            throw new OcCapacityBizException("산정 요청이 비어 있습니다.");
        }
        int totalUsers = request.resolvedTotalUsers();
        if (totalUsers <= 0) {
            throw new OcCapacityBizException("전체 사용자 수는 1 이상이어야 합니다.");
        }
        List<Double> rates = normalizeRates(request.getConcurrentRequestRates());
        List<Integer> timeouts = normalizeTimeouts(request.getTargetResponseTimes());
        if (rates.isEmpty() || timeouts.isEmpty()) {
            throw new OcCapacityBizException("동시요청률과 목표 응답시간을 1개 이상 선택하세요.");
        }
        VmProfile profile = resolveVmProfile(request.getVmSpecCode());
        int tpsPerCore = request.getTpsPerCore() > 0
                ? request.getTpsPerCore()
                : NsightCapacityDerivation.coreTpsFromTpmc(request.getTpmcPerTps()).tpsPerCoreBase();
        int vmTps = profile.getCores() * tpsPerCore;
        double margin = request.getSessionMarginRate() > 1
                ? request.getSessionMarginRate() / 100.0
                : request.getSessionMarginRate();
        int designedSessions = (int) Math.ceil(totalUsers * (1.0 + Math.max(0, margin)));
        boolean singleView = isSingleView(request.getApType());
        var spec = profile.getTomcatHikariSpec();
        int profileCap = singleView ? spec.hikariSingleViewMax() : spec.hikariGeneralMax();
        double holdSec = request.getAvgDbConnectionHoldSec() > 0
                ? request.getAvgDbConnectionHoldSec()
                : NsightDbPoolDerivation.defaultHoldSec(singleView);

        return new CapacityCalculationDDTO(
                request.getProjectName(),
                request.getBranchCount(),
                request.getUserPerBranch(),
                totalUsers,
                designedSessions,
                margin,
                request.getSessionTimeoutMin(),
                rates,
                timeouts,
                profile.getId(),
                profile.getCores(),
                profile.getMemoryGb(),
                vmTps,
                tpsPerCore,
                Math.max(1, request.getTpmcPerTps()),
                Math.max(0.5, request.getAvgThreadHoldSec()),
                Math.max(1.0, request.getThreadMarginRate()),
                Math.max(1.0, request.getMaxThreadMarginRate()),
                singleView,
                request.isActiveActive(),
                request.isDrValidation(),
                request.isValidateDbPool(),
                request.getDbSessionLimit() > 0 ? request.getDbSessionLimit() : 500,
                holdSec,
                request.getDbTransactionUsageRatio() > 0 ? request.getDbTransactionUsageRatio() : 1.0,
                request.getPoolSafetyFactor() > 0 ? request.getPoolSafetyFactor() : 1.3,
                request.getThreadDbUsageRatio() > 0 ? request.getThreadDbUsageRatio() : 0.30,
                request.getMinPoolPerVm() > 0 ? request.getMinPoolPerVm() : 30,
                profileCap);
    }

    public static CapacityCalculationCDTO defaultRequest() {
        CapacityCalculationCDTO dto = new CapacityCalculationCDTO();
        dto.setProjectName("6,000지점 표준 시나리오");
        dto.setBranchCount(6000);
        dto.setUserPerBranch(6);
        dto.setSessionMarginRate(0.30);
        dto.setSessionTimeoutMin(60);
        dto.setConcurrentRequestRates(new ArrayList<>(List.of(0.03, 0.05, 0.10, 0.15)));
        dto.setTargetResponseTimes(new ArrayList<>(List.of(3, 4, 5)));
        dto.setVmSpecCode("8C64G");
        dto.setTpsPerCore(35);
        dto.setTpmcPerTps(3000);
        dto.setAvgThreadHoldSec(1.2);
        dto.setThreadMarginRate(1.2);
        dto.setMaxThreadMarginRate(1.3);
        dto.setApType("GENERAL");
        dto.setActiveActive(true);
        dto.setDrValidation(true);
        dto.setValidateDbPool(true);
        dto.setDbSessionLimit(500);
        dto.setAvgDbConnectionHoldSec(0.15);
        dto.setDbTransactionUsageRatio(1.0);
        dto.setPoolSafetyFactor(1.3);
        dto.setThreadDbUsageRatio(0.30);
        dto.setMinPoolPerVm(30);
        return dto;
    }

    public static VmProfile resolveVmProfile(String vmSpecCode) {
        if (vmSpecCode == null || vmSpecCode.isBlank()) {
            return VmProfile.CORE8_64;
        }
        String normalized = vmSpecCode.trim().toUpperCase().replace("-", "").replace("_", "");
        return switch (normalized) {
            case "2C16G", "2CORE16GB", "2CORE-16GB" -> VmProfile.CORE2_16;
            case "4C32G", "4CORE32GB", "4CORE-32GB" -> VmProfile.CORE4_32;
            case "8C64G", "8CORE64GB", "8CORE-64GB" -> VmProfile.CORE8_64;
            case "16C128G", "16CORE128GB", "16CORE-128GB" -> VmProfile.CORE16_128;
            case "32C256G", "32CORE256GB", "32CORE-256GB" -> VmProfile.CORE32_256;
            case "8C32G", "8CORE32GB", "8CORE-32GB" -> VmProfile.CORE8_32;
            case "16C64G", "16CORE64GB", "16CORE-64GB" -> VmProfile.CORE16_64;
            default -> {
                String resolved = VmProfile.normalizeProfileId(vmSpecCode);
                if (resolved != null) {
                    yield profileById(resolved);
                }
                yield VmProfile.CORE8_64;
            }
        };
    }

    private static VmProfile profileById(String id) {
        for (VmProfile profile : VmProfile.values()) {
            if (profile.getId().equalsIgnoreCase(id)) {
                return profile;
            }
        }
        return VmProfile.CORE8_64;
    }

    private static boolean isSingleView(String apType) {
        return apType != null && apType.toUpperCase().contains("SINGLE");
    }

    private static List<Double> normalizeRates(List<Double> rates) {
        if (rates == null || rates.isEmpty()) {
            return List.of(0.03, 0.05, 0.10, 0.15);
        }
        Set<Double> ordered = new LinkedHashSet<>();
        for (Double rate : rates) {
            if (rate == null) {
                continue;
            }
            double v = rate > 1 ? rate / 100.0 : rate;
            if (v > 0 && v <= 1) {
                ordered.add(v);
            }
        }
        return ordered.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private static List<Integer> normalizeTimeouts(List<Integer> timeouts) {
        if (timeouts == null || timeouts.isEmpty()) {
            return List.of(3, 4, 5);
        }
        return timeouts.stream().filter(t -> t != null && t > 0).sorted().distinct().toList();
    }
}
