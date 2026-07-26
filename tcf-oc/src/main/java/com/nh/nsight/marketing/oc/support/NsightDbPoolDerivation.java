package com.nh.nsight.marketing.oc.support;

/**
 * NSIGHT 용량산정 DB Pool — 2026-06-03 DB Pool 설계서.
 * <p>① AP TPS → ② 산출 = AP TPS × hold × DB비율 × Pool안전 → ③ 상한 = Thread × DB% →
 * ⑤ 용량 권장 = min(②, ③) → ④ 배포 Pool = max(운영최소, ⑤) (프로파일 cap 가능)</p>
 */
public final class NsightDbPoolDerivation {

    public static final double DEFAULT_DB_HOLD_SEC_GENERAL = 0.15;
    public static final double DEFAULT_DB_HOLD_SEC_SINGLE_VIEW = 0.20;
    public static final double DEFAULT_DB_USAGE_RATIO = 1.0;
    public static final double DEFAULT_POOL_SAFETY_FACTOR = 1.3;
    public static final double DEFAULT_THREAD_DB_USAGE_RATIO = 0.30;
    public static final int DEFAULT_MIN_POOL = 30;

    public record Input(
            double apTps,
            int threadsPerVm,
            double dbConnectionHoldSec,
            double dbUsageRatio,
            double poolSafetyFactor,
            double threadDbUsageRatio,
            int minPool,
            int profilePoolCap
    ) {
    }

    public record Result(
            int apTpsRounded,
            int theoreticalPool,
            int ceilingPool,
            /** min(②, ③) — 용량 관점 권장 (운영 최소 미적용) */
            int sizedPool,
            /** max(운영최소, ⑤) — Hikari maximumPoolSize 배포값 */
            int recommendedPool,
            double threadPoolRatio,
            String formulaSummary
    ) {
    }

    private NsightDbPoolDerivation() {
    }

    public static double defaultHoldSec(boolean singleView) {
        return singleView ? DEFAULT_DB_HOLD_SEC_SINGLE_VIEW : DEFAULT_DB_HOLD_SEC_GENERAL;
    }

    public static Result recommend(Input input) {
        double apTps = Math.max(0, input.apTps());
        int threads = Math.max(1, input.threadsPerVm());
        double hold = Math.max(0.01, input.dbConnectionHoldSec());
        double usage = clamp(input.dbUsageRatio(), 0.1, 1.0);
        double safety = Math.max(1.0, input.poolSafetyFactor());
        double threadUsage = clamp(input.threadDbUsageRatio(), 0.1, 1.0);
        int minPool = Math.max(1, input.minPool());

        int theoretical = (int) Math.ceil(apTps * hold * usage * safety);
        int ceiling = (int) Math.ceil(threads * threadUsage);
        int sized = Math.min(theoretical, ceiling);
        int recommended = Math.max(minPool, sized);
        if (input.profilePoolCap() > 0) {
            recommended = Math.min(recommended, input.profilePoolCap());
        }

        double ratio = recommended > 0 ? (double) threads / recommended : 0;
        String formula = String.format(
                "①AP TPS≈%.0f · ②산출→%d · ③상한=Thread%d×%.0f%%→%d · ⑤용량=min(%d,%d)=%d · ④배포=max(%d,⑤)=%d",
                apTps, theoretical,
                threads, threadUsage * 100, ceiling,
                theoretical, ceiling, sized,
                minPool, recommended);

        return new Result(
                (int) Math.ceil(apTps),
                theoretical,
                ceiling,
                sized,
                recommended,
                Math.round(ratio * 10) / 10.0,
                formula
        );
    }

    private static double clamp(double v, double min, double max) {
        return Math.min(max, Math.max(min, v));
    }
}
