package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CapNewCompareCDTO {

    private List<String> scenarioIds = new ArrayList<>();
    private String baselineScenarioId;
    private List<Map<String, Object>> columns = new ArrayList<>();
    private List<Map<String, Object>> metricRows = new ArrayList<>();
    private List<String> diffHighlights = new ArrayList<>();
    private String recommendation;
    private String summary;

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

    public List<Map<String, Object>> getColumns() {
        return columns;
    }

    public void setColumns(List<Map<String, Object>> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getMetricRows() {
        return metricRows;
    }

    public void setMetricRows(List<Map<String, Object>> metricRows) {
        this.metricRows = metricRows;
    }

    public List<String> getDiffHighlights() {
        return diffHighlights;
    }

    public void setDiffHighlights(List<String> diffHighlights) {
        this.diffHighlights = diffHighlights;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
