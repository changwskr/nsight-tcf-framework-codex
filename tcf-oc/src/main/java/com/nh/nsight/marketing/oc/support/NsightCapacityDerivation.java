package com.nh.nsight.marketing.oc.support;

/**
 * 용량산정 문서 기준 파생: 실요청 사용자(동시) ↔ TPS ↔ AP 분산.
 * <p>5% 피크: 전체 36,000명 → 실요청 1,800명 → TPS = 1,800 ÷ p95(3초) = 600 (전사) → AP 2대 시 AP당 900 동시.</p>
 */
public final class NsightCapacityDerivation {

    /** 기준: TPMC 3,000/TPS 일 때 Core당 30~40 TPS → Core TPMC/초 = TPS/Core × TPMC/TPS */
    public static final int REF_TPMC_PER_TPS = 3_000;
    public static final int REF_TPS_PER_CORE_MIN = 30;
    public static final int REF_TPS_PER_CORE_BASE = 35;
    public static final int REF_TPS_PER_CORE_MAX = 40;
    public static final int REF_CORE_TPMC_PER_SEC = REF_TPS_PER_CORE_BASE * REF_TPMC_PER_TPS;

    /**
     * Core당 TPS는 1 TPS당 TPMC와 역연동.
     * Core TPMC/초(처리 여력) ≈ 고정 → TPS/Core = Core TPMC/초 ÷ TPMC/TPS
     */
    public record CoreTpsFromTpmc(
            int tpsPerCoreMin,
            int tpsPerCoreBase,
            int tpsPerCoreMax,
            int coreTpmcPerSec,
            int tpmcPerTps
    ) {
    }

    public record CapacityTargets(
            int actualRequestUsers,
            int actualRequestPeakPercent,
            int peakTpsFromActualRequest,
            int concurrentTotal,
            int concurrentPerAp,
            int configuredPeakTps,
            int p95Ms,
            int apCount
    ) {
        public boolean peakTpsMatchesActualRequest() {
            return configuredPeakTps == peakTpsFromActualRequest;
        }
    }

    private NsightCapacityDerivation() {
    }

    /** TPS(전사) = 실요청 동시 사용자 ÷ p95(초) */
    public static int peakTpsFromActualRequestUsers(int actualRequestUsers, int p95Ms) {
        if (p95Ms <= 0) {
            return 0;
        }
        return (int) Math.ceil(actualRequestUsers / (p95Ms / 1000.0));
    }

    /** AP당 동시 처리 목표 = 실요청(전사 동시) ÷ AP 대수 */
    public static int concurrentPerAp(int actualRequestUsers, int apCount) {
        return (int) Math.ceil((double) actualRequestUsers / Math.max(1, apCount));
    }

    public static int expectedActualRequestFromPercent(int totalUsers, int peakPercent) {
        return (int) Math.ceil(totalUsers * (peakPercent / 100.0));
    }

    /**
     * TPMC/TPS 변경 시 Core당 TPS 자동 산출 (기준 35 TPS @ 3,000 TPMC).
     */
    public static CoreTpsFromTpmc coreTpsFromTpmc(int tpmcPerTps) {
        int safeTpmc = Math.max(1, tpmcPerTps);
        int min = (int) Math.floor((double) REF_TPS_PER_CORE_MIN * REF_TPMC_PER_TPS / safeTpmc);
        int base = (int) Math.floor((double) REF_TPS_PER_CORE_BASE * REF_TPMC_PER_TPS / safeTpmc);
        int max = (int) Math.floor((double) REF_TPS_PER_CORE_MAX * REF_TPMC_PER_TPS / safeTpmc);
        int coreTpmc = base * safeTpmc;
        return new CoreTpsFromTpmc(
                Math.max(1, min),
                Math.max(1, base),
                Math.max(min, max),
                coreTpmc,
                safeTpmc
        );
    }

    /** Core TPS 직접 입력 시 역산 TPMC/TPS (검증용). */
    public static int tpmcPerTpsFromCoreBase(int tpsPerCoreBase) {
        if (tpsPerCoreBase <= 0) {
            return REF_TPMC_PER_TPS;
        }
        return (int) Math.round((double) REF_CORE_TPMC_PER_SEC / tpsPerCoreBase);
    }

    public static CapacityTargets resolve(
            int totalUsers,
            int actualRequestUsers,
            int actualRequestPeakPercent,
            int configuredPeakTps,
            int p95Ms,
            int apCount
    ) {
        int safeAp = Math.max(1, apCount);
        int derivedTps = peakTpsFromActualRequestUsers(actualRequestUsers, p95Ms);
        return new CapacityTargets(
                actualRequestUsers,
                actualRequestPeakPercent,
                derivedTps,
                actualRequestUsers,
                concurrentPerAp(actualRequestUsers, safeAp),
                configuredPeakTps,
                p95Ms,
                safeAp
        );
    }
}
