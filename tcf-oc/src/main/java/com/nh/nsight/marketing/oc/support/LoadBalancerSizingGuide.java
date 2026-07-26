package com.nh.nsight.marketing.oc.support;

/**
 * GSLB / L4 / Load Balancer 설정 — 32CORE·256GB 권장(용량산정 문서).
 * <p>대형 VM은 장애 영향이 크므로 Health 5초·Idle 70~90초·Sticky 70~80분·
 * Least/Weighted Least Connection 분산을 권장합니다.</p>
 */
public final class LoadBalancerSizingGuide {

    public static final int DNS_TTL_SEC_MIN = 30;
    public static final int DNS_TTL_SEC_MAX = 60;
    public static final int HEALTH_INTERVAL_SEC = 5;
    public static final int HEALTH_TIMEOUT_SEC = 2;
    public static final String HEALTH_FAIL_COUNT_RANGE = "2~3";
    public static final int HEALTH_FAIL_COUNT_DEFAULT = 3;

    private LoadBalancerSizingGuide() {
    }

    public record ProfileSpec(
            String dnsTtl,
            String healthCheckRecommended,
            int healthIntervalSec,
            int healthTimeoutSec,
            String healthFailCountRange,
            String stickyPersistence,
            String stickyTimeoutRange,
            int stickyTimeoutSecDefault,
            String clientL4IdleRange,
            int clientL4IdleSecDefault,
            String l4WasIdleRange,
            int l4WasIdleSecDefault,
            String loadBalancingMethod,
            String maxConnectionGuidance,
            String tomcatKeepAliveNote
    ) {
        public String healthCheckFormula() {
            return "Interval " + healthIntervalSec + "s · Timeout " + healthTimeoutSec
                    + "s · Fail " + healthFailCountRange + "회";
        }
    }

    /**
     * @param sessionIdleMinutes 세션 Idle(분) — Sticky = max(세션+10분, 70)~80분
     * @param apCountPerCenter    센터당 AP(VM) 대수 — L4 Max Connection 검증용 (기본 2)
     * @param tomcatMaxConnections VM당 Tomcat maxConnections 상한
     */
    public static ProfileSpec specFor(
            VmProfile profile,
            int sessionIdleMinutes,
            int apCountPerCenter,
            int tomcatMaxConnections
    ) {
        VmProfile p = profile != null ? profile : VmProfile.defaultProfile();
        int stickyLow = Math.max(70, sessionIdleMinutes + 10);
        int stickyHigh = sessionIdleMinutes <= 60
                ? 80
                : stickyLow + 10;
        String stickyRange = stickyLow + "~" + stickyHigh + "분";
        int stickySec = ((stickyLow + stickyHigh) / 2) * 60;
        int safeAp = Math.max(1, apCountPerCenter);
        int safeTomcatConn = Math.max(1, tomcatMaxConnections);
        long l4MaxConnFloor = (long) safeAp * safeTomcatConn;
        String maxConnGuide = "≥ " + l4MaxConnFloor + " (센터 AP " + safeAp + "대 × VM maxConnections "
                + safeTomcatConn + ")";

        if (p == VmProfile.CORE32_256) {
            return new ProfileSpec(
                    "30~60초",
                    HEALTH_INTERVAL_SEC + "s / " + HEALTH_TIMEOUT_SEC + "s / " + HEALTH_FAIL_COUNT_RANGE + "회",
                    HEALTH_INTERVAL_SEC,
                    HEALTH_TIMEOUT_SEC,
                    HEALTH_FAIL_COUNT_RANGE,
                    "적용 (JSESSIONID 또는 Source IP · DeltaManager 복제 부하 감소)",
                    stickyRange,
                    stickySec,
                    "70~90초",
                    80,
                    "70~90초",
                    80,
                    "Least Connection 또는 Weighted Least Connection",
                    maxConnGuide,
                    "Tomcat keepAliveTimeout 60초보다 길게 (L4-WAS Idle 70~90초)"
            );
        }
        return new ProfileSpec(
                "30~60초",
                HEALTH_INTERVAL_SEC + "s / " + HEALTH_TIMEOUT_SEC + "s / " + HEALTH_FAIL_COUNT_RANGE + "회",
                HEALTH_INTERVAL_SEC,
                HEALTH_TIMEOUT_SEC,
                HEALTH_FAIL_COUNT_RANGE,
                "적용 (JSESSIONID 또는 Source IP)",
                stickyRange,
                stickySec,
                "120초",
                120,
                "120초",
                120,
                "Round Robin 또는 Least Connection",
                maxConnGuide,
                "Apache/Tomcat KeepAlive ≤ L4 Idle"
        );
    }

    public static ProfileSpec specFor(VmProfile profile, int sessionIdleMinutes) {
        int tomcatMaxConn = profile != null
                ? profile.getTomcatHikariSpec().maxConnectionsMax()
                : 12_000;
        return specFor(profile, sessionIdleMinutes, Nsight32Core256GbGuide.DEFAULT_AP_COUNT, tomcatMaxConn);
    }

    public static ProfileSpec specFor(int cores, int memoryGb, int sessionIdleMinutes) {
        return specFor(VmProfile.nearest(cores, memoryGb), sessionIdleMinutes);
    }
}
