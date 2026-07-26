package com.nh.nsight.marketing.oc.capnew.persistence.dto;

public class CapNewScenarioTemplateRow {

    private String templateCode;
    private String label;
    private String description;
    private String purpose;
    private String targetEnv;
    private String vmProfileCode;
    private int totalUsers;
    private int designPeakTps;
    private int deploymentAp;
    private int maxThreads;
    private int poolPerVm;
    private int sortOrder;
    private String enabled;
    private String seedPayload;
    private String createdAt;
    private String updatedAt;

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getTargetEnv() {
        return targetEnv;
    }

    public void setTargetEnv(String targetEnv) {
        this.targetEnv = targetEnv;
    }

    public String getVmProfileCode() {
        return vmProfileCode;
    }

    public void setVmProfileCode(String vmProfileCode) {
        this.vmProfileCode = vmProfileCode;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getDesignPeakTps() {
        return designPeakTps;
    }

    public void setDesignPeakTps(int designPeakTps) {
        this.designPeakTps = designPeakTps;
    }

    public int getDeploymentAp() {
        return deploymentAp;
    }

    public void setDeploymentAp(int deploymentAp) {
        this.deploymentAp = deploymentAp;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getPoolPerVm() {
        return poolPerVm;
    }

    public void setPoolPerVm(int poolPerVm) {
        this.poolPerVm = poolPerVm;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getSeedPayload() {
        return seedPayload;
    }

    public void setSeedPayload(String seedPayload) {
        this.seedPayload = seedPayload;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
