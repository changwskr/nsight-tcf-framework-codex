package com.nh.nsight.marketing.oc.config;

import com.nh.nsight.marketing.oc.support.Nsight32Core256GbGuide;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.env-check")
public class EnvCheckProperties {

    private String defaultProjectId = "nsight-message-mgmt";
    private String defaultProjectName = "NSIGHT Message Management";
    private String defaultEnvCode = "local";
    private String hardwareProfile = Nsight32Core256GbGuide.PROFILE_ID;
    private String centerType = "DR";
    private int branchCount = Nsight32Core256GbGuide.BRANCH_COUNT;
    private int usersPerBranch = Nsight32Core256GbGuide.USERS_PER_BRANCH;
    private int totalUsers = Nsight32Core256GbGuide.TOTAL_USERS;
    private int actualRequestUsers = Nsight32Core256GbGuide.ACTUAL_REQUEST_USERS_PEAK;
    private int actualRequestPeakPercent = Nsight32Core256GbGuide.ACTUAL_REQUEST_PERCENT_PEAK;
    private int sessionDesignCount = Nsight32Core256GbGuide.SESSION_DESIGN_COUNT;
    private int sessionBufferedMin = Nsight32Core256GbGuide.SESSION_BUFFERED_MIN;
    private int sessionBufferedMax = Nsight32Core256GbGuide.SESSION_BUFFERED_MAX;
    private int baseTps = Nsight32Core256GbGuide.LOW_LOAD_TPS;
    private int peakTps = Nsight32Core256GbGuide.PEAK_TPS;
    private int highPeakTps = Nsight32Core256GbGuide.HIGH_PEAK_TPS;
    private int stressTps = Nsight32Core256GbGuide.STRESS_TPS;
    private int vmMaxTps = Nsight32Core256GbGuide.VM_MAX_TPS;
    private int peakConcurrentUsers = Nsight32Core256GbGuide.PEAK_CONCURRENT_USERS;
    private int apCount = Nsight32Core256GbGuide.DEFAULT_AP_COUNT;
    private String apVmSpec = Nsight32Core256GbGuide.VM_SPEC;
    private int targetP95Ms = Nsight32Core256GbGuide.TARGET_P95_MS;
    private int l4IdleTimeoutMs = Nsight32Core256GbGuide.L4_CLIENT_IDLE_TIMEOUT_SEC * 1000;
    private int l4HealthIntervalSec = Nsight32Core256GbGuide.L4_HEALTH_INTERVAL_SEC;
    private int l4HealthTimeoutSec = Nsight32Core256GbGuide.L4_HEALTH_TIMEOUT_SEC;
    private int l4HealthFailCount = Nsight32Core256GbGuide.L4_HEALTH_FAIL_COUNT;
    private int l4StickyTimeoutSec = Nsight32Core256GbGuide.L4_STICKY_TIMEOUT_SEC;
    private int gslbHealthIntervalSec = Nsight32Core256GbGuide.GSLB_HEALTH_INTERVAL_SEC;
    private int gslbHealthTimeoutSec = Nsight32Core256GbGuide.GSLB_HEALTH_TIMEOUT_SEC;
    private int gslbHealthFailCount = Nsight32Core256GbGuide.GSLB_HEALTH_FAIL_COUNT;
    private int gslbStickyTimeoutSec = Nsight32Core256GbGuide.GSLB_STICKY_TIMEOUT_SEC;
    private int proxyReadTimeoutMs = Nsight32Core256GbGuide.PROXY_READ_TIMEOUT_MS;
    private int dbSessionLimit = Nsight32Core256GbGuide.DB_SESSION_LIMIT_REF;
    private int tomcatMaxThreads = Nsight32Core256GbGuide.TOMCAT_MAX_THREADS;
    private int tomcatAcceptCount = Nsight32Core256GbGuide.TOMCAT_ACCEPT_COUNT;
    private int tomcatMaxConnections = Nsight32Core256GbGuide.TOMCAT_MAX_CONNECTIONS;
    private int hikariPoolGeneral = Nsight32Core256GbGuide.HIKARI_POOL_GENERAL;

    public String getDefaultProjectId() {
        return defaultProjectId;
    }

    public void setDefaultProjectId(String defaultProjectId) {
        this.defaultProjectId = defaultProjectId;
    }

    public String getDefaultProjectName() {
        return defaultProjectName;
    }

    public void setDefaultProjectName(String defaultProjectName) {
        this.defaultProjectName = defaultProjectName;
    }

    public String getDefaultEnvCode() {
        return defaultEnvCode;
    }

    public void setDefaultEnvCode(String defaultEnvCode) {
        this.defaultEnvCode = defaultEnvCode;
    }

    public String getHardwareProfile() {
        return hardwareProfile;
    }

    public void setHardwareProfile(String hardwareProfile) {
        this.hardwareProfile = hardwareProfile;
    }

    public String getCenterType() {
        return centerType;
    }

    public void setCenterType(String centerType) {
        this.centerType = centerType;
    }

    public int getBranchCount() {
        return branchCount;
    }

    public void setBranchCount(int branchCount) {
        this.branchCount = branchCount;
    }

    public int getUsersPerBranch() {
        return usersPerBranch;
    }

    public void setUsersPerBranch(int usersPerBranch) {
        this.usersPerBranch = usersPerBranch;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getActualRequestUsers() {
        return actualRequestUsers;
    }

    public void setActualRequestUsers(int actualRequestUsers) {
        this.actualRequestUsers = actualRequestUsers;
    }

    public int getActualRequestPeakPercent() {
        return actualRequestPeakPercent;
    }

    public void setActualRequestPeakPercent(int actualRequestPeakPercent) {
        this.actualRequestPeakPercent = actualRequestPeakPercent;
    }

    public int getSessionDesignCount() {
        return sessionDesignCount;
    }

    public void setSessionDesignCount(int sessionDesignCount) {
        this.sessionDesignCount = sessionDesignCount;
    }

    public int getSessionBufferedMin() {
        return sessionBufferedMin;
    }

    public void setSessionBufferedMin(int sessionBufferedMin) {
        this.sessionBufferedMin = sessionBufferedMin;
    }

    public int getSessionBufferedMax() {
        return sessionBufferedMax;
    }

    public void setSessionBufferedMax(int sessionBufferedMax) {
        this.sessionBufferedMax = sessionBufferedMax;
    }

    public int getBaseTps() {
        return baseTps;
    }

    public void setBaseTps(int baseTps) {
        this.baseTps = baseTps;
    }

    public int getPeakTps() {
        return peakTps;
    }

    public void setPeakTps(int peakTps) {
        this.peakTps = peakTps;
    }

    public int getHighPeakTps() {
        return highPeakTps;
    }

    public void setHighPeakTps(int highPeakTps) {
        this.highPeakTps = highPeakTps;
    }

    public int getStressTps() {
        return stressTps;
    }

    public void setStressTps(int stressTps) {
        this.stressTps = stressTps;
    }

    public int getVmMaxTps() {
        return vmMaxTps;
    }

    public void setVmMaxTps(int vmMaxTps) {
        this.vmMaxTps = vmMaxTps;
    }

    public int getPeakConcurrentUsers() {
        return peakConcurrentUsers;
    }

    public void setPeakConcurrentUsers(int peakConcurrentUsers) {
        this.peakConcurrentUsers = peakConcurrentUsers;
    }

    public int getApCount() {
        return apCount;
    }

    public void setApCount(int apCount) {
        this.apCount = apCount;
    }

    public String getApVmSpec() {
        return apVmSpec;
    }

    public void setApVmSpec(String apVmSpec) {
        this.apVmSpec = apVmSpec;
    }

    public int getTargetP95Ms() {
        return targetP95Ms;
    }

    public void setTargetP95Ms(int targetP95Ms) {
        this.targetP95Ms = targetP95Ms;
    }

    public int getL4IdleTimeoutMs() {
        return l4IdleTimeoutMs;
    }

    public void setL4IdleTimeoutMs(int l4IdleTimeoutMs) {
        this.l4IdleTimeoutMs = l4IdleTimeoutMs;
    }

    public int getL4HealthIntervalSec() {
        return l4HealthIntervalSec;
    }

    public void setL4HealthIntervalSec(int l4HealthIntervalSec) {
        this.l4HealthIntervalSec = l4HealthIntervalSec;
    }

    public int getL4HealthTimeoutSec() {
        return l4HealthTimeoutSec;
    }

    public void setL4HealthTimeoutSec(int l4HealthTimeoutSec) {
        this.l4HealthTimeoutSec = l4HealthTimeoutSec;
    }

    public int getL4HealthFailCount() {
        return l4HealthFailCount;
    }

    public void setL4HealthFailCount(int l4HealthFailCount) {
        this.l4HealthFailCount = l4HealthFailCount;
    }

    public int getL4StickyTimeoutSec() {
        return l4StickyTimeoutSec;
    }

    public void setL4StickyTimeoutSec(int l4StickyTimeoutSec) {
        this.l4StickyTimeoutSec = l4StickyTimeoutSec;
    }

    public int getGslbHealthIntervalSec() {
        return gslbHealthIntervalSec;
    }

    public void setGslbHealthIntervalSec(int gslbHealthIntervalSec) {
        this.gslbHealthIntervalSec = gslbHealthIntervalSec;
    }

    public int getGslbHealthTimeoutSec() {
        return gslbHealthTimeoutSec;
    }

    public void setGslbHealthTimeoutSec(int gslbHealthTimeoutSec) {
        this.gslbHealthTimeoutSec = gslbHealthTimeoutSec;
    }

    public int getGslbHealthFailCount() {
        return gslbHealthFailCount;
    }

    public void setGslbHealthFailCount(int gslbHealthFailCount) {
        this.gslbHealthFailCount = gslbHealthFailCount;
    }

    public int getGslbStickyTimeoutSec() {
        return gslbStickyTimeoutSec;
    }

    public void setGslbStickyTimeoutSec(int gslbStickyTimeoutSec) {
        this.gslbStickyTimeoutSec = gslbStickyTimeoutSec;
    }

    public int getProxyReadTimeoutMs() {
        return proxyReadTimeoutMs;
    }

    public void setProxyReadTimeoutMs(int proxyReadTimeoutMs) {
        this.proxyReadTimeoutMs = proxyReadTimeoutMs;
    }

    public int getDbSessionLimit() {
        return dbSessionLimit;
    }

    public void setDbSessionLimit(int dbSessionLimit) {
        this.dbSessionLimit = dbSessionLimit;
    }

    public int getTomcatMaxThreads() {
        return tomcatMaxThreads;
    }

    public void setTomcatMaxThreads(int tomcatMaxThreads) {
        this.tomcatMaxThreads = tomcatMaxThreads;
    }

    public int getTomcatAcceptCount() {
        return tomcatAcceptCount;
    }

    public void setTomcatAcceptCount(int tomcatAcceptCount) {
        this.tomcatAcceptCount = tomcatAcceptCount;
    }

    public int getTomcatMaxConnections() {
        return tomcatMaxConnections;
    }

    public void setTomcatMaxConnections(int tomcatMaxConnections) {
        this.tomcatMaxConnections = tomcatMaxConnections;
    }

    public int getHikariPoolGeneral() {
        return hikariPoolGeneral;
    }

    public void setHikariPoolGeneral(int hikariPoolGeneral) {
        this.hikariPoolGeneral = hikariPoolGeneral;
    }
}
