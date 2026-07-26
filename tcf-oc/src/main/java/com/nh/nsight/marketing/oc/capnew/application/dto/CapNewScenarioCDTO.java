package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CapNewScenarioCDTO {

    private String scenarioId;
    private String projectId;
    private String projectName;
    private String scenarioName;
    private String targetEnv;
    private String baseDate;
    private String versionNo;
    private String author;
    private String description;
    private String purpose;
    private String status;
    private int currentStep;
    private Map<String, Object> stepPayload = new LinkedHashMap<>();
    private List<CapNewStepTrackStatusCDTO> stepTrack = new ArrayList<>();
    private CapNewStepValidationCDTO lastValidation;
    private CapNewCascadeImpactCDTO cascadeImpact;
    private String createdAt;
    private String updatedAt;

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public Map<String, Object> getStepPayload() {
        return stepPayload;
    }

    public void setStepPayload(Map<String, Object> stepPayload) {
        this.stepPayload = stepPayload;
    }

    public List<CapNewStepTrackStatusCDTO> getStepTrack() {
        return stepTrack;
    }

    public void setStepTrack(List<CapNewStepTrackStatusCDTO> stepTrack) {
        this.stepTrack = stepTrack;
    }

    public CapNewStepValidationCDTO getLastValidation() {
        return lastValidation;
    }

    public void setLastValidation(CapNewStepValidationCDTO lastValidation) {
        this.lastValidation = lastValidation;
    }

    public CapNewCascadeImpactCDTO getCascadeImpact() {
        return cascadeImpact;
    }

    public void setCascadeImpact(CapNewCascadeImpactCDTO cascadeImpact) {
        this.cascadeImpact = cascadeImpact;
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
