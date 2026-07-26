package com.nh.nsight.marketing.sv.application.dto.sample;

import com.nh.nsight.marketing.sv.persistence.dto.sample.SampleRow;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 샘플 목록 한 행 (응답용).
 */
public class SampleListItem {

    private final String sampleKey;
    private final String sampleName;
    private final String createdAt;

    public SampleListItem(String sampleKey, String sampleName, String createdAt) {
        this.sampleKey = sampleKey;
        this.sampleName = sampleName;
        this.createdAt = createdAt;
    }

    public static SampleListItem fromRow(SampleRow row) {
        return new SampleListItem(row.getSampleKey(), row.getSampleName(), row.getCreatedAt());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sampleKey", sampleKey);
        map.put("sampleName", sampleName);
        map.put("createdAt", createdAt);
        return map;
    }
}
