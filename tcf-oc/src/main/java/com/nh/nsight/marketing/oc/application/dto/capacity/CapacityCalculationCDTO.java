package com.nh.nsight.marketing.oc.application.dto.capacity;

import java.util.ArrayList;
import java.util.List;

/** 설계서 CAP-010 입력 (POST /api/oc/capacity/calculate). */
public class CapacityCalculationCDTO {

    private String projectName;
    private int branchCount = 6000;
    private int userPerBranch = 6;
    private double sessionMarginRate = 0.30;
    private int sessionTimeoutMin = 60;
    private List<Double> concurrentRequestRates = new ArrayList<>(List.of(0.03, 0.05, 0.10, 0.15));
    private List<Integer> targetResponseTimes = new ArrayList<>(List.of(3, 4, 5));
    private String vmSpecCode = "8C64G";
    private int tpsPerCore = 35;
    private int tpmcPerTps = 3000;
    private double avgThreadHoldSec = 1.2;
    private double threadMarginRate = 1.2;
    private double maxThreadMarginRate = 1.3;
    private String apType = "GENERAL";
    private boolean activeActive = true;
    private boolean drValidation = true;
    private boolean validateDbPool = true;
    private int dbSessionLimit = 500;
    /** DB Pool 설계서 — 평균 DB Connection 점유(초). 미입력 시 AP 유형별 기본(0.15/0.20). */
    private double avgDbConnectionHoldSec;
    private double dbTransactionUsageRatio = 1.0;
    private double poolSafetyFactor = 1.3;
    private double threadDbUsageRatio = 0.30;
    private int minPoolPerVm = 30;
    /** 단계별 산정 시 020|030|040|050|ALL (POST /calculate-step). */
    private String calculationStep;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getBranchCount() {
        return branchCount;
    }

    public void setBranchCount(int branchCount) {
        this.branchCount = branchCount;
    }

    public int getUserPerBranch() {
        return userPerBranch;
    }

    public void setUserPerBranch(int userPerBranch) {
        this.userPerBranch = userPerBranch;
    }

    public double getSessionMarginRate() {
        return sessionMarginRate;
    }

    public void setSessionMarginRate(double sessionMarginRate) {
        this.sessionMarginRate = sessionMarginRate;
    }

    public int getSessionTimeoutMin() {
        return sessionTimeoutMin;
    }

    public void setSessionTimeoutMin(int sessionTimeoutMin) {
        this.sessionTimeoutMin = sessionTimeoutMin;
    }

    public List<Double> getConcurrentRequestRates() {
        return concurrentRequestRates;
    }

    public void setConcurrentRequestRates(List<Double> concurrentRequestRates) {
        this.concurrentRequestRates = concurrentRequestRates;
    }

    public List<Integer> getTargetResponseTimes() {
        return targetResponseTimes;
    }

    public void setTargetResponseTimes(List<Integer> targetResponseTimes) {
        this.targetResponseTimes = targetResponseTimes;
    }

    public String getVmSpecCode() {
        return vmSpecCode;
    }

    public void setVmSpecCode(String vmSpecCode) {
        this.vmSpecCode = vmSpecCode;
    }

    public int getTpsPerCore() {
        return tpsPerCore;
    }

    public void setTpsPerCore(int tpsPerCore) {
        this.tpsPerCore = tpsPerCore;
    }

    public int getTpmcPerTps() {
        return tpmcPerTps;
    }

    public void setTpmcPerTps(int tpmcPerTps) {
        this.tpmcPerTps = tpmcPerTps;
    }

    public double getAvgThreadHoldSec() {
        return avgThreadHoldSec;
    }

    public void setAvgThreadHoldSec(double avgThreadHoldSec) {
        this.avgThreadHoldSec = avgThreadHoldSec;
    }

    public double getThreadMarginRate() {
        return threadMarginRate;
    }

    public void setThreadMarginRate(double threadMarginRate) {
        this.threadMarginRate = threadMarginRate;
    }

    public double getMaxThreadMarginRate() {
        return maxThreadMarginRate;
    }

    public void setMaxThreadMarginRate(double maxThreadMarginRate) {
        this.maxThreadMarginRate = maxThreadMarginRate;
    }

    public String getApType() {
        return apType;
    }

    public void setApType(String apType) {
        this.apType = apType;
    }

    public boolean isActiveActive() {
        return activeActive;
    }

    public void setActiveActive(boolean activeActive) {
        this.activeActive = activeActive;
    }

    public boolean isDrValidation() {
        return drValidation;
    }

    public void setDrValidation(boolean drValidation) {
        this.drValidation = drValidation;
    }

    public boolean isValidateDbPool() {
        return validateDbPool;
    }

    public void setValidateDbPool(boolean validateDbPool) {
        this.validateDbPool = validateDbPool;
    }

    public int getDbSessionLimit() {
        return dbSessionLimit;
    }

    public void setDbSessionLimit(int dbSessionLimit) {
        this.dbSessionLimit = dbSessionLimit;
    }

    public double getAvgDbConnectionHoldSec() {
        return avgDbConnectionHoldSec;
    }

    public void setAvgDbConnectionHoldSec(double avgDbConnectionHoldSec) {
        this.avgDbConnectionHoldSec = avgDbConnectionHoldSec;
    }

    public double getDbTransactionUsageRatio() {
        return dbTransactionUsageRatio;
    }

    public void setDbTransactionUsageRatio(double dbTransactionUsageRatio) {
        this.dbTransactionUsageRatio = dbTransactionUsageRatio;
    }

    public double getPoolSafetyFactor() {
        return poolSafetyFactor;
    }

    public void setPoolSafetyFactor(double poolSafetyFactor) {
        this.poolSafetyFactor = poolSafetyFactor;
    }

    public double getThreadDbUsageRatio() {
        return threadDbUsageRatio;
    }

    public void setThreadDbUsageRatio(double threadDbUsageRatio) {
        this.threadDbUsageRatio = threadDbUsageRatio;
    }

    public int getMinPoolPerVm() {
        return minPoolPerVm;
    }

    public void setMinPoolPerVm(int minPoolPerVm) {
        this.minPoolPerVm = minPoolPerVm;
    }

    public String getCalculationStep() {
        return calculationStep;
    }

    public void setCalculationStep(String calculationStep) {
        this.calculationStep = calculationStep;
    }

    public int resolvedTotalUsers() {
        return Math.max(0, branchCount * userPerBranch);
    }
}
