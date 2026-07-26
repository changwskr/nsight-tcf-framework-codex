package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CapNewDefaultsCDTO {

    private Map<String, Object> step1 = new LinkedHashMap<>();
    private Map<String, Object> step2 = new LinkedHashMap<>();
    private Map<String, Object> step4 = new LinkedHashMap<>();
    private Map<String, Object> step5 = new LinkedHashMap<>();
    private Map<String, Object> step6 = new LinkedHashMap<>();
    private Map<String, Object> step7 = new LinkedHashMap<>();
    private List<Map<String, Object>> tpsPresets = new ArrayList<>();
    private List<Map<String, Object>> vmProfiles = new ArrayList<>();
    private List<Map<String, Object>> businessTypes = new ArrayList<>();
    private List<Map<String, Object>> centerModes = new ArrayList<>();
    private List<String> targetEnvs = new ArrayList<>();
    private List<String> purposes = new ArrayList<>();
    private List<Map<String, Object>> warPresets = new ArrayList<>();

    public Map<String, Object> getStep1() {
        return step1;
    }

    public void setStep1(Map<String, Object> step1) {
        this.step1 = step1;
    }

    public Map<String, Object> getStep2() {
        return step2;
    }

    public void setStep2(Map<String, Object> step2) {
        this.step2 = step2;
    }

    public Map<String, Object> getStep4() {
        return step4;
    }

    public void setStep4(Map<String, Object> step4) {
        this.step4 = step4;
    }

    public Map<String, Object> getStep5() {
        return step5;
    }

    public void setStep5(Map<String, Object> step5) {
        this.step5 = step5;
    }

    public Map<String, Object> getStep6() {
        return step6;
    }

    public void setStep6(Map<String, Object> step6) {
        this.step6 = step6;
    }

    public Map<String, Object> getStep7() {
        return step7;
    }

    public void setStep7(Map<String, Object> step7) {
        this.step7 = step7;
    }

    public List<Map<String, Object>> getCenterModes() {
        return centerModes;
    }

    public void setCenterModes(List<Map<String, Object>> centerModes) {
        this.centerModes = centerModes;
    }

    public List<Map<String, Object>> getTpsPresets() {
        return tpsPresets;
    }

    public void setTpsPresets(List<Map<String, Object>> tpsPresets) {
        this.tpsPresets = tpsPresets;
    }

    public List<Map<String, Object>> getVmProfiles() {
        return vmProfiles;
    }

    public void setVmProfiles(List<Map<String, Object>> vmProfiles) {
        this.vmProfiles = vmProfiles;
    }

    public List<Map<String, Object>> getBusinessTypes() {
        return businessTypes;
    }

    public void setBusinessTypes(List<Map<String, Object>> businessTypes) {
        this.businessTypes = businessTypes;
    }

    public List<String> getTargetEnvs() {
        return targetEnvs;
    }

    public void setTargetEnvs(List<String> targetEnvs) {
        this.targetEnvs = targetEnvs;
    }

    public List<String> getPurposes() {
        return purposes;
    }

    public void setPurposes(List<String> purposes) {
        this.purposes = purposes;
    }

    public List<Map<String, Object>> getWarPresets() {
        return warPresets;
    }

    public void setWarPresets(List<Map<String, Object>> warPresets) {
        this.warPresets = warPresets;
    }
}
