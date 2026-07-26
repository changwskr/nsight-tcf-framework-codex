package com.nh.nsight.marketing.oc.support;

/**
 * §4 Tomcat Thread 및 HikariCP 기준 — 용량산정 문서(2026-05-31).
 */
public final class TomcatHikariSizingGuide {

    public static final String OPERATIONAL_NOTE = TomcatWasSizingGuide.OPERATIONAL_NOTE;

    private TomcatHikariSizingGuide() {
    }

    /**
     * 프로파일별 Tomcat·Hikari 권장 범위 (ENV-003·ENV-004·산정 공통).
     */
    public record ProfileSpec(
            int tomcatMaxThreadsMin,
            int tomcatMaxThreadsMax,
            int minSpareThreadsMin,
            int minSpareThreadsMax,
            int acceptCountMin,
            int acceptCountMax,
            int maxConnectionsMin,
            int maxConnectionsMax,
            int hikariGeneralMin,
            int hikariGeneralMax,
            int hikariSingleViewMin,
            int hikariSingleViewMax,
            int guideTpsForBusy,
            int connectionTimeoutSec,
            int keepAliveTimeoutSec,
            int maxKeepAliveRequests,
            String threadStackXss,
            String cautionNote
    ) {
        public String tomcatMaxThreadsRange() {
            return tomcatMaxThreadsMin + "~" + tomcatMaxThreadsMax;
        }

        public String tomcatMaxThreadsDisplay() {
            return "보수 " + tomcatMaxThreadsMin + " · 권장 " + tomcatMaxThreadsRange();
        }

        public String minSpareThreadsRange() {
            return minSpareThreadsMin + "~" + minSpareThreadsMax;
        }

        public String minSpareThreadsDisplay() {
            return "보수 " + minSpareThreadsMin + " · 권장 " + minSpareThreadsMax;
        }

        public String acceptCountRange() {
            return acceptCountMin + "~" + acceptCountMax;
        }

        public String acceptCountDisplay() {
            return "보수 " + acceptCountMin + " · 권장 " + acceptCountRange();
        }

        public String maxConnectionsRange() {
            return maxConnectionsMin + "~" + maxConnectionsMax;
        }

        public String maxConnectionsDisplay() {
            return "보수 " + maxConnectionsMin + " · 권장 " + maxConnectionsRange();
        }

        public String hikariGeneralRange() {
            return hikariGeneralMin + "~" + hikariGeneralMax;
        }

        public String hikariSingleViewRange() {
            return hikariSingleViewMin + "~" + hikariSingleViewMax;
        }

        public int hikariGeneralRecommended() {
            return hikariGeneralMax;
        }

        public int busyThreadsLow() {
            return TomcatWasSizingGuide.busyThreadsLow(guideTpsForBusy);
        }

        public int busyThreadsHigh() {
            return TomcatWasSizingGuide.busyThreadsHigh(guideTpsForBusy);
        }

        public String busyThreadFormula() {
            return TomcatWasSizingGuide.busyThreadFormula(guideTpsForBusy);
        }

        public String hikariGeneralFormula(VmProfile profile) {
            return "Hikari 일반 " + hikariGeneralRange() + " (" + profile.getId()
                    + " · 권장 " + hikariGeneralRecommended() + ")";
        }
    }

    public static ProfileSpec specFor(VmProfile profile) {
        return profile.getTomcatHikariSpec();
    }

    public static ProfileSpec specFor(int cores, int memoryGb) {
        return specFor(VmProfile.nearest(cores, memoryGb));
    }
}
