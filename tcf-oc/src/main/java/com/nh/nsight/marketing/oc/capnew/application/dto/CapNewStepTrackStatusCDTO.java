package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.List;

public class CapNewStepTrackStatusCDTO {

    private int step;
    private String label;
    /** active | done | warn | error | pending */
    private String state;
    /** ● ✓ ! × ○ */
    private String symbol;
    private String hint;
    private List<String> issues = new ArrayList<>();

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public void addIssue(String issue) {
        issues.add(issue);
    }
}
