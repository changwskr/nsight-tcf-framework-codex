package com.nh.nsight.marketing.oc.application.dto.capacity;

public class CapacityScenarioResultCDTO {

    private double concurrentRate;
    private int responseTimeSec;
    private int concurrentRequestUsers;
    private int targetTps;
    private long requiredTpmc;
    private int requiredCore;
    private int requiredApCount;
    private int recommendedApCount;
    /** WAS·DB 산정용 배포 AP (A-A 시 필요 AP×2) */
    private int deploymentApCount;
    /** 가용 TPS = 필요 AP × VM TPS(기준) = AP대수 × VM코어 × Core TPS */
    private int availableTps;
    private int vmTpsAtBase;
    private String vmProfileId;
    private WasThreadResultCDTO wasThread;
    private DbPoolResultCDTO dbPool;
    private String tpsStatus;
    private String tpsStatusReason;

    public double getConcurrentRate() {
        return concurrentRate;
    }

    public void setConcurrentRate(double concurrentRate) {
        this.concurrentRate = concurrentRate;
    }

    public int getResponseTimeSec() {
        return responseTimeSec;
    }

    public void setResponseTimeSec(int responseTimeSec) {
        this.responseTimeSec = responseTimeSec;
    }

    public int getConcurrentRequestUsers() {
        return concurrentRequestUsers;
    }

    public void setConcurrentRequestUsers(int concurrentRequestUsers) {
        this.concurrentRequestUsers = concurrentRequestUsers;
    }

    public int getTargetTps() {
        return targetTps;
    }

    public void setTargetTps(int targetTps) {
        this.targetTps = targetTps;
    }

    public long getRequiredTpmc() {
        return requiredTpmc;
    }

    public void setRequiredTpmc(long requiredTpmc) {
        this.requiredTpmc = requiredTpmc;
    }

    public int getRequiredCore() {
        return requiredCore;
    }

    public void setRequiredCore(int requiredCore) {
        this.requiredCore = requiredCore;
    }

    public int getRequiredApCount() {
        return requiredApCount;
    }

    public void setRequiredApCount(int requiredApCount) {
        this.requiredApCount = requiredApCount;
    }

    public int getRecommendedApCount() {
        return recommendedApCount;
    }

    public void setRecommendedApCount(int recommendedApCount) {
        this.recommendedApCount = recommendedApCount;
    }

    public int getDeploymentApCount() {
        return deploymentApCount;
    }

    public void setDeploymentApCount(int deploymentApCount) {
        this.deploymentApCount = deploymentApCount;
    }

    public int getAvailableTps() {
        return availableTps;
    }

    public void setAvailableTps(int availableTps) {
        this.availableTps = availableTps;
    }

    public int getVmTpsAtBase() {
        return vmTpsAtBase;
    }

    public void setVmTpsAtBase(int vmTpsAtBase) {
        this.vmTpsAtBase = vmTpsAtBase;
    }

    public String getVmProfileId() {
        return vmProfileId;
    }

    public void setVmProfileId(String vmProfileId) {
        this.vmProfileId = vmProfileId;
    }

    public WasThreadResultCDTO getWasThread() {
        return wasThread;
    }

    public void setWasThread(WasThreadResultCDTO wasThread) {
        this.wasThread = wasThread;
    }

    public DbPoolResultCDTO getDbPool() {
        return dbPool;
    }

    public void setDbPool(DbPoolResultCDTO dbPool) {
        this.dbPool = dbPool;
    }

    public String getTpsStatus() {
        return tpsStatus;
    }

    public void setTpsStatus(String tpsStatus) {
        this.tpsStatus = tpsStatus;
    }

    public String getTpsStatusReason() {
        return tpsStatusReason;
    }

    public void setTpsStatusReason(String tpsStatusReason) {
        this.tpsStatusReason = tpsStatusReason;
    }
}
