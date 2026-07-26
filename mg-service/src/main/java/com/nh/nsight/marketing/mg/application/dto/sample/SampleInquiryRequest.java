package com.nh.nsight.marketing.mg.application.dto.sample;

import java.util.Map;

public class SampleInquiryRequest {
    private final String sampleKey;
    public SampleInquiryRequest(String sampleKey) { this.sampleKey = sampleKey; }
    public static SampleInquiryRequest fromMap(Map<String, Object> body) {
        if (body == null) return null;
        Object raw = body.get("sampleKey");
        String sampleKey = raw == null ? null : String.valueOf(raw).trim();
        return new SampleInquiryRequest(sampleKey == null || sampleKey.isEmpty() ? null : sampleKey);
    }
    public String getSampleKey() { return sampleKey; }
}
