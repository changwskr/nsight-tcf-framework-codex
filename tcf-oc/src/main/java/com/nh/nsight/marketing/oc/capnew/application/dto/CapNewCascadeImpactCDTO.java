package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.List;

public class CapNewCascadeImpactCDTO {

    private int sourceStep;
    private String sourceStepLabel;
    private boolean recalculated;
    private List<Integer> affectedSteps = new ArrayList<>();
    private List<String> affectedStepLabels = new ArrayList<>();
    private List<ChangeItem> changes = new ArrayList<>();
    private String summary;
    private String message;

    public static class ChangeItem {
        private String fieldId;
        private String label;
        private String beforeValue;
        private String afterValue;
        private int affectedStep;

        public String getFieldId() {
            return fieldId;
        }

        public void setFieldId(String fieldId) {
            this.fieldId = fieldId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getBeforeValue() {
            return beforeValue;
        }

        public void setBeforeValue(String beforeValue) {
            this.beforeValue = beforeValue;
        }

        public String getAfterValue() {
            return afterValue;
        }

        public void setAfterValue(String afterValue) {
            this.afterValue = afterValue;
        }

        public int getAffectedStep() {
            return affectedStep;
        }

        public void setAffectedStep(int affectedStep) {
            this.affectedStep = affectedStep;
        }
    }

    public int getSourceStep() {
        return sourceStep;
    }

    public void setSourceStep(int sourceStep) {
        this.sourceStep = sourceStep;
    }

    public String getSourceStepLabel() {
        return sourceStepLabel;
    }

    public void setSourceStepLabel(String sourceStepLabel) {
        this.sourceStepLabel = sourceStepLabel;
    }

    public boolean isRecalculated() {
        return recalculated;
    }

    public void setRecalculated(boolean recalculated) {
        this.recalculated = recalculated;
    }

    public List<Integer> getAffectedSteps() {
        return affectedSteps;
    }

    public void setAffectedSteps(List<Integer> affectedSteps) {
        this.affectedSteps = affectedSteps;
    }

    public List<String> getAffectedStepLabels() {
        return affectedStepLabels;
    }

    public void setAffectedStepLabels(List<String> affectedStepLabels) {
        this.affectedStepLabels = affectedStepLabels;
    }

    public List<ChangeItem> getChanges() {
        return changes;
    }

    public void setChanges(List<ChangeItem> changes) {
        this.changes = changes;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
