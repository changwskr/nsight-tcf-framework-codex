package com.nh.nsight.marketing.oc.application.dto.capacity;

public class DbPoolResultCDTO {

    private int apTpsPerVm;
    private int poolTheoretical;
    private int poolCeiling;
    /** min(②, ③) — 용량 권장 (운영 최소 미적용) */
    private int poolSized;
    private int poolPerVm;
    /** Pool 산정에 사용한 AP 대수 (A-A 시 배포 대수) */
    private int apCountForPool;
    private boolean minPoolFloorApplied;
    private long totalDbSessions;
    private double threadPoolRatio;
    private String poolFormula;
    private String status;
    private String statusMessage;

    public int getApTpsPerVm() {
        return apTpsPerVm;
    }

    public void setApTpsPerVm(int apTpsPerVm) {
        this.apTpsPerVm = apTpsPerVm;
    }

    public int getPoolTheoretical() {
        return poolTheoretical;
    }

    public void setPoolTheoretical(int poolTheoretical) {
        this.poolTheoretical = poolTheoretical;
    }

    public int getPoolCeiling() {
        return poolCeiling;
    }

    public void setPoolCeiling(int poolCeiling) {
        this.poolCeiling = poolCeiling;
    }

    public int getPoolSized() {
        return poolSized;
    }

    public void setPoolSized(int poolSized) {
        this.poolSized = poolSized;
    }

    public int getApCountForPool() {
        return apCountForPool;
    }

    public void setApCountForPool(int apCountForPool) {
        this.apCountForPool = apCountForPool;
    }

    public boolean isMinPoolFloorApplied() {
        return minPoolFloorApplied;
    }

    public void setMinPoolFloorApplied(boolean minPoolFloorApplied) {
        this.minPoolFloorApplied = minPoolFloorApplied;
    }

    public int getPoolPerVm() {
        return poolPerVm;
    }

    public void setPoolPerVm(int poolPerVm) {
        this.poolPerVm = poolPerVm;
    }

    public long getTotalDbSessions() {
        return totalDbSessions;
    }

    public void setTotalDbSessions(long totalDbSessions) {
        this.totalDbSessions = totalDbSessions;
    }

    public String getPoolFormula() {
        return poolFormula;
    }

    public void setPoolFormula(String poolFormula) {
        this.poolFormula = poolFormula;
    }

    public double getThreadPoolRatio() {
        return threadPoolRatio;
    }

    public void setThreadPoolRatio(double threadPoolRatio) {
        this.threadPoolRatio = threadPoolRatio;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
