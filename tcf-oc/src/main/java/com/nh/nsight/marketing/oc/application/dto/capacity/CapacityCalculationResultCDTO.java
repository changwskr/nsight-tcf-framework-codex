package com.nh.nsight.marketing.oc.application.dto.capacity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CapacityCalculationResultCDTO {

    private String scenarioId;
    private String scenarioLabel;
    private int totalUserCount;
    private int designedSessionCount;
    private String vmProfileId;
    private int vmCores;
    private int vmMemoryGb;
    private int vmTpsAtBase;
    private String summaryFormula;
    private Map<String, Integer> riskSummary = new LinkedHashMap<>();
    private List<CapacityScenarioResultCDTO> results = new ArrayList<>();
    /** 마지막 산정 단계 코드 (020, 030, 040, 050, ALL). */
    private String calculatedStep;
    private String calculatedStepLabel;

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioLabel() {
        return scenarioLabel;
    }

    public void setScenarioLabel(String scenarioLabel) {
        this.scenarioLabel = scenarioLabel;
    }

    public int getTotalUserCount() {
        return totalUserCount;
    }

    public void setTotalUserCount(int totalUserCount) {
        this.totalUserCount = totalUserCount;
    }

    public int getDesignedSessionCount() {
        return designedSessionCount;
    }

    public void setDesignedSessionCount(int designedSessionCount) {
        this.designedSessionCount = designedSessionCount;
    }

    public String getVmProfileId() {
        return vmProfileId;
    }

    public void setVmProfileId(String vmProfileId) {
        this.vmProfileId = vmProfileId;
    }

    public int getVmCores() {
        return vmCores;
    }

    public void setVmCores(int vmCores) {
        this.vmCores = vmCores;
    }

    public int getVmMemoryGb() {
        return vmMemoryGb;
    }

    public void setVmMemoryGb(int vmMemoryGb) {
        this.vmMemoryGb = vmMemoryGb;
    }

    public int getVmTpsAtBase() {
        return vmTpsAtBase;
    }

    public void setVmTpsAtBase(int vmTpsAtBase) {
        this.vmTpsAtBase = vmTpsAtBase;
    }

    public String getSummaryFormula() {
        return summaryFormula;
    }

    public void setSummaryFormula(String summaryFormula) {
        this.summaryFormula = summaryFormula;
    }

    public Map<String, Integer> getRiskSummary() {
        return riskSummary;
    }

    public void setRiskSummary(Map<String, Integer> riskSummary) {
        this.riskSummary = riskSummary;
    }

    public List<CapacityScenarioResultCDTO> getResults() {
        return results;
    }

    public void setResults(List<CapacityScenarioResultCDTO> results) {
        this.results = results;
    }

    public String getCalculatedStep() {
        return calculatedStep;
    }

    public void setCalculatedStep(String calculatedStep) {
        this.calculatedStep = calculatedStep;
    }

    public String getCalculatedStepLabel() {
        return calculatedStepLabel;
    }

    public void setCalculatedStepLabel(String calculatedStepLabel) {
        this.calculatedStepLabel = calculatedStepLabel;
    }
}
