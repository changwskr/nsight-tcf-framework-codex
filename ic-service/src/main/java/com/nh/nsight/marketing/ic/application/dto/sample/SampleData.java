package com.nh.nsight.marketing.ic.application.dto.sample;

import com.nh.nsight.marketing.ic.persistence.dto.sample.SampleRow;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IC 샘플 조회 data 영역.
 */
public class SampleData {

    private final String sampleKey;
    private final String sampleName;
    private final String database;
    private final String createdAt;

    public SampleData(String sampleKey, String sampleName, String database, String createdAt) {
        this.sampleKey = sampleKey;
        this.sampleName = sampleName;
        this.database = database;
        this.createdAt = createdAt;
    }

    public static SampleData fromRow(SampleRow row) {
        return new SampleData(row.getSampleKey(), row.getSampleName(), row.getDatabase(), row.getCreatedAt());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sampleKey", sampleKey);
        map.put("sampleName", sampleName);
        map.put("database", database);
        map.put("createdAt", createdAt);
        return map;
    }
}
