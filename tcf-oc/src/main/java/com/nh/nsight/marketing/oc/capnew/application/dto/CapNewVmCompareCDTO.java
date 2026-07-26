package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.List;

public class CapNewVmCompareCDTO {

    private String scenarioId;
    private String scenarioName;
    private String baselineCode;
    private String baselineLabel;
    private int baselineTps;
    private String currentVmProfileCode;
    private List<VmAlternativeRow> alternatives = new ArrayList<>();
    private String recommendation;
    private String summary;

    public static class VmAlternativeRow {
        private String vmProfileCode;
        private String vmProfileLabel;
        private int vmCores;
        private int vmMemoryGb;
        private int vmTheoreticalTps;
        private int vmAdjustedTps;
        private int apPerCenterNormal;
        private int apPerCenterFailover;
        private String requiredApDisplay;
        private int totalDeploymentAp;
        private int totalCores;
        private String failureBlastRadius;
        private String failureBlastLabel;
        private String judgment;
        private String apJudgment;
        private boolean selected;

        public String getVmProfileCode() {
            return vmProfileCode;
        }

        public void setVmProfileCode(String vmProfileCode) {
            this.vmProfileCode = vmProfileCode;
        }

        public String getVmProfileLabel() {
            return vmProfileLabel;
        }

        public void setVmProfileLabel(String vmProfileLabel) {
            this.vmProfileLabel = vmProfileLabel;
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

        public int getVmTheoreticalTps() {
            return vmTheoreticalTps;
        }

        public void setVmTheoreticalTps(int vmTheoreticalTps) {
            this.vmTheoreticalTps = vmTheoreticalTps;
        }

        public int getVmAdjustedTps() {
            return vmAdjustedTps;
        }

        public void setVmAdjustedTps(int vmAdjustedTps) {
            this.vmAdjustedTps = vmAdjustedTps;
        }

        public int getApPerCenterNormal() {
            return apPerCenterNormal;
        }

        public void setApPerCenterNormal(int apPerCenterNormal) {
            this.apPerCenterNormal = apPerCenterNormal;
        }

        public int getApPerCenterFailover() {
            return apPerCenterFailover;
        }

        public void setApPerCenterFailover(int apPerCenterFailover) {
            this.apPerCenterFailover = apPerCenterFailover;
        }

        public String getRequiredApDisplay() {
            return requiredApDisplay;
        }

        public void setRequiredApDisplay(String requiredApDisplay) {
            this.requiredApDisplay = requiredApDisplay;
        }

        public int getTotalDeploymentAp() {
            return totalDeploymentAp;
        }

        public void setTotalDeploymentAp(int totalDeploymentAp) {
            this.totalDeploymentAp = totalDeploymentAp;
        }

        public int getTotalCores() {
            return totalCores;
        }

        public void setTotalCores(int totalCores) {
            this.totalCores = totalCores;
        }

        public String getFailureBlastRadius() {
            return failureBlastRadius;
        }

        public void setFailureBlastRadius(String failureBlastRadius) {
            this.failureBlastRadius = failureBlastRadius;
        }

        public String getFailureBlastLabel() {
            return failureBlastLabel;
        }

        public void setFailureBlastLabel(String failureBlastLabel) {
            this.failureBlastLabel = failureBlastLabel;
        }

        public String getJudgment() {
            return judgment;
        }

        public void setJudgment(String judgment) {
            this.judgment = judgment;
        }

        public String getApJudgment() {
            return apJudgment;
        }

        public void setApJudgment(String apJudgment) {
            this.apJudgment = apJudgment;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
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

    public int getBaselineTps() {
        return baselineTps;
    }

    public void setBaselineTps(int baselineTps) {
        this.baselineTps = baselineTps;
    }

    public String getCurrentVmProfileCode() {
        return currentVmProfileCode;
    }

    public void setCurrentVmProfileCode(String currentVmProfileCode) {
        this.currentVmProfileCode = currentVmProfileCode;
    }

    public List<VmAlternativeRow> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<VmAlternativeRow> alternatives) {
        this.alternatives = alternatives;
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
