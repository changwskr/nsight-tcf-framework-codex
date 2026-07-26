package com.nh.nsight.marketing.oc.capnew.application.dto;

import com.nh.nsight.marketing.oc.application.dto.env.CapacityDesignView;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerRequest;

public class CapNewEnvHandoffCDTO {

    private String capNewScenarioId;
    private String capNewScenarioName;
    private String projectName;
    private String targetEnv;
    private String envPageUrl;
    private String handoffAt;
    private String note;
    private CapacityPlannerRequest capacityRequest;
    private CapacityDesignView capacityView;

    public String getCapNewScenarioId() {
        return capNewScenarioId;
    }

    public void setCapNewScenarioId(String capNewScenarioId) {
        this.capNewScenarioId = capNewScenarioId;
    }

    public String getCapNewScenarioName() {
        return capNewScenarioName;
    }

    public void setCapNewScenarioName(String capNewScenarioName) {
        this.capNewScenarioName = capNewScenarioName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTargetEnv() {
        return targetEnv;
    }

    public void setTargetEnv(String targetEnv) {
        this.targetEnv = targetEnv;
    }

    public String getEnvPageUrl() {
        return envPageUrl;
    }

    public void setEnvPageUrl(String envPageUrl) {
        this.envPageUrl = envPageUrl;
    }

    public String getHandoffAt() {
        return handoffAt;
    }

    public void setHandoffAt(String handoffAt) {
        this.handoffAt = handoffAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public CapacityPlannerRequest getCapacityRequest() {
        return capacityRequest;
    }

    public void setCapacityRequest(CapacityPlannerRequest capacityRequest) {
        this.capacityRequest = capacityRequest;
    }

    public CapacityDesignView getCapacityView() {
        return capacityView;
    }

    public void setCapacityView(CapacityDesignView capacityView) {
        this.capacityView = capacityView;
    }
}
