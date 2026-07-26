package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.List;

public class CapNewExcelExportRequest {

    private String exportType = "SCENARIO";
    private String scenarioId;
    private List<String> scenarioIds = new ArrayList<>();
    private String baselineScenarioId;

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public List<String> getScenarioIds() {
        return scenarioIds;
    }

    public void setScenarioIds(List<String> scenarioIds) {
        this.scenarioIds = scenarioIds;
    }

    public String getBaselineScenarioId() {
        return baselineScenarioId;
    }

    public void setBaselineScenarioId(String baselineScenarioId) {
        this.baselineScenarioId = baselineScenarioId;
    }
}
