package com.nh.nsight.marketing.pd.persistence.dto.sample;

public class SampleRow {
    private String sampleKey, sampleName, database, createdAt;
    public String getSampleKey() { return sampleKey; }
    public void setSampleKey(String sampleKey) { this.sampleKey = sampleKey; }
    public String getSampleName() { return sampleName; }
    public void setSampleName(String sampleName) { this.sampleName = sampleName; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
