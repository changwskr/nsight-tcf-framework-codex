package com.nh.nsight.marketing.oc.support;

/**
 * Spring Boot / Spring Framework — {@code application.yml} 기준 (32CORE/256GB 재점검).
 * <p>Embedded Tomcat·세션·비동기 Executor·트랜잭션 Timeout을 한 계층에서 관리합니다.</p>
 */
public final class SpringBootSizingGuide {

    public static final String OPERATIONAL_NOTE =
            "성능시험에서 Tomcat maxThreads 최종 보정 · Async는 업무 Thread와 분리";

    private SpringBootSizingGuide() {
    }

    /**
     * 프로파일별 Spring Boot 권장 (Tomcat embedded + 세션 + Async + TX).
     */
    public record ProfileSpec(
            TomcatHikariSizingGuide.ProfileSpec tomcat,
            int asyncCoreMin,
            int asyncCoreMax,
            int asyncMaxPoolMin,
            int asyncMaxPoolMax,
            int transactionTimeoutSecMin,
            int transactionTimeoutSecMax,
            String asyncExecutorKeyPrefix
    ) {
        public String tomcatMaxThreadsRange() {
            return tomcat.tomcatMaxThreadsRange();
        }

        public String tomcatMaxThreadsDisplay() {
            return tomcat.tomcatMaxThreadsDisplay();
        }

        public String minSpareThreadsRange() {
            return tomcat.minSpareThreadsRange();
        }

        public String acceptCountRange() {
            return tomcat.acceptCountRange();
        }

        public String maxConnectionsRange() {
            return tomcat.maxConnectionsRange();
        }

        public int connectionTimeoutSec() {
            return tomcat.connectionTimeoutSec();
        }

        public int keepAliveTimeoutSec() {
            return tomcat.keepAliveTimeoutSec();
        }

        public String asyncCorePoolRange() {
            return asyncCoreMin + "~" + asyncCoreMax;
        }

        public String asyncMaxPoolRange() {
            return asyncMaxPoolMin + "~" + asyncMaxPoolMax;
        }

        public String transactionTimeoutRange() {
            return transactionTimeoutSecMin + "~" + transactionTimeoutSecMax + "s";
        }

        public String asyncCorePoolKey() {
            return asyncExecutorKeyPrefix + ".core-pool-size";
        }

        public String asyncMaxPoolKey() {
            return asyncExecutorKeyPrefix + ".max-pool-size";
        }
    }

    public static ProfileSpec specFor(VmProfile profile) {
        TomcatHikariSizingGuide.ProfileSpec th = profile.getTomcatHikariSpec();
        String asyncPrefix = "nsight.async.audit-log";
        return switch (profile) {
            case CORE32_256 -> new ProfileSpec(
                    th,
                    Nsight32Core256GbGuide.ASYNC_CORE_POOL_MIN,
                    Nsight32Core256GbGuide.ASYNC_CORE_POOL_MAX,
                    Nsight32Core256GbGuide.ASYNC_MAX_POOL_MIN,
                    Nsight32Core256GbGuide.ASYNC_MAX_POOL_MAX,
                    Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MIN,
                    Nsight32Core256GbGuide.TRANSACTION_TIMEOUT_SEC_MAX,
                    asyncPrefix);
            case CORE16_64, CORE16_128 -> new ProfileSpec(
                    th, 30, 60, 60, 120, 4, 5, asyncPrefix);
            default -> new ProfileSpec(th, 20, 40, 40, 80, 4, 5, asyncPrefix);
        };
    }

    public static ProfileSpec specFor(int cores, int memoryGb) {
        return specFor(VmProfile.nearest(cores, memoryGb));
    }

    public static String sizingSummary(VmProfile profile, int sessionMinutes) {
        ProfileSpec s = specFor(profile);
        return "session " + sessionMinutes + "m · TX " + s.transactionTimeoutRange()
                + " · Async core " + s.asyncCorePoolRange();
    }
}
