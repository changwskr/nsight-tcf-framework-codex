package com.nh.nsight.marketing.av.persistence.dto.sample;

import java.util.LinkedHashMap;
import java.util.Map;

public class SampleRow {

    private String sampleKey;
    private String sampleName;
    private String useYn;
    private String createdAt;

    public String getSampleKey() {
        return sampleKey;
    }

    public void setSampleKey(String sampleKey) {
        this.sampleKey = sampleKey;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sampleKey", sampleKey);
        map.put("sampleName", sampleName);
        map.put("useYn", useYn);
        map.put("createdAt", createdAt);
        return map;
    }
}
