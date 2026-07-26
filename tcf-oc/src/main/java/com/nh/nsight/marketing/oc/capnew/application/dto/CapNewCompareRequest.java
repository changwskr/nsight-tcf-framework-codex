package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.List;

public class CapNewCompareRequest {

    private List<String> scenarioIds = new ArrayList<>();
    private String baselineScenarioId;

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
