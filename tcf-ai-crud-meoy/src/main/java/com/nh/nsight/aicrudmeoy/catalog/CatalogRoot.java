package com.nh.nsight.aicrudmeoy.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogRoot {
    private String version;
    private String sourceNote;
    private String masterId;
    private List<StepDefinition> steps = new ArrayList<>();

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getSourceNote() { return sourceNote; }
    public void setSourceNote(String sourceNote) { this.sourceNote = sourceNote; }
    public String getMasterId() { return masterId; }
    public void setMasterId(String masterId) { this.masterId = masterId; }
    public List<StepDefinition> getSteps() { return steps; }
    public void setSteps(List<StepDefinition> steps) { this.steps = steps; }
}
