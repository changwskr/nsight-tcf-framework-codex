package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.List;

public class CapNewLegacyCompareCDTO {

    private String scenarioId;
    private String scenarioName;
    private String baselineCode;
    private String baselineLabel;
    private double baselineConcurrentRate;
    private int baselineResponseSec;
    /** MATCH | PARTIAL | MISMATCH */
    private String overallStatus;
    private String summary;
    private List<String> notes = new ArrayList<>();
    private List<MetricRow> metrics = new ArrayList<>();

    public static class MetricRow {
        private String metricId;
        private String metricLabel;
        private String unit;
        private Object capNewValue;
        private Object legacyValue;
        private Object diff;
        /** MATCH | CLOSE | DIFF */
        private String status;

        public String getMetricId() {
            return metricId;
        }

        public void setMetricId(String metricId) {
            this.metricId = metricId;
        }

        public String getMetricLabel() {
            return metricLabel;
        }

        public void setMetricLabel(String metricLabel) {
            this.metricLabel = metricLabel;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Object getCapNewValue() {
            return capNewValue;
        }

        public void setCapNewValue(Object capNewValue) {
            this.capNewValue = capNewValue;
        }

        public Object getLegacyValue() {
            return legacyValue;
        }

        public void setLegacyValue(Object legacyValue) {
            this.legacyValue = legacyValue;
        }

        public Object getDiff() {
            return diff;
        }

        public void setDiff(Object diff) {
            this.diff = diff;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getBaselineCode() {
        return baselineCode;
    }

    public void setBaselineCode(String baselineCode) {
        this.baselineCode = baselineCode;
    }

    public String getBaselineLabel() {
        return baselineLabel;
    }

    public void setBaselineLabel(String baselineLabel) {
        this.baselineLabel = baselineLabel;
    }

    public double getBaselineConcurrentRate() {
        return baselineConcurrentRate;
    }

    public void setBaselineConcurrentRate(double baselineConcurrentRate) {
        this.baselineConcurrentRate = baselineConcurrentRate;
    }

    public int getBaselineResponseSec() {
        return baselineResponseSec;
    }

    public void setBaselineResponseSec(int baselineResponseSec) {
        this.baselineResponseSec = baselineResponseSec;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public List<MetricRow> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricRow> metrics) {
        this.metrics = metrics;
    }
}
