package com.nh.nsight.marketing.pd.application.dto.sample;

import com.nh.nsight.marketing.pd.persistence.dto.sample.SampleRow;
import java.util.LinkedHashMap;
import java.util.Map;

public class SampleData {
    private final String sampleKey, sampleName, database, createdAt;
    public SampleData(String sampleKey, String sampleName, String database, String createdAt) {
        this.sampleKey = sampleKey; this.sampleName = sampleName; this.database = database; this.createdAt = createdAt;
    }
    public static SampleData fromRow(SampleRow row) {
        return new SampleData(row.getSampleKey(), row.getSampleName(), row.getDatabase(), row.getCreatedAt());
    }
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sampleKey", sampleKey); map.put("sampleName", sampleName);
        map.put("database", database); map.put("createdAt", createdAt);
        return map;
    }
}
