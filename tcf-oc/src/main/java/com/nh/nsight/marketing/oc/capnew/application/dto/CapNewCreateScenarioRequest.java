package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class CapNewCreateScenarioRequest {

    private String projectId;
    private String projectName;
    private String scenarioName;
    private String targetEnv;
    private String baseDate;
    private String versionNo;
    private String author;
    private String description;
    private String purpose;
    private String templateCode;

    public Map<String, Object> toStep1Map() {
        Map<String, Object> step1 = new LinkedHashMap<>();
        step1.put("projectId", projectId);
        step1.put("projectName", projectName);
        step1.put("scenarioName", scenarioName);
        step1.put("targetEnv", targetEnv);
        step1.put("baseDate", baseDate);
        step1.put("versionNo", versionNo);
        step1.put("author", author);
        step1.put("description", description);
        step1.put("purpose", purpose);
        return step1;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getTargetEnv() {
        return targetEnv;
    }

    public void setTargetEnv(String targetEnv) {
        this.targetEnv = targetEnv;
    }

    public String getBaseDate() {
        return baseDate;
    }

    public void setBaseDate(String baseDate) {
        this.baseDate = baseDate;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }
}
