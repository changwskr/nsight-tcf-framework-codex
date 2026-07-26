package com.nh.nsight.marketing.sv.persistence.dto.sample;

/**
 * SV_SAMPLE 조회 Row (MyBatis result).
 */
public class SampleRow {

    private String sampleKey;
    private String sampleName;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
