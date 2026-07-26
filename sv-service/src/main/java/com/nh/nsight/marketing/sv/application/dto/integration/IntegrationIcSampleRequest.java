package com.nh.nsight.marketing.sv.application.dto.integration;

import java.util.Map;

/**
 * SV.Integration.icSample 요청 body.
 */
public class IntegrationIcSampleRequest {

    private static final String DEFAULT_SAMPLE_KEY = "SV-CALL";

    private final String sampleKey;

    public IntegrationIcSampleRequest(String sampleKey) {
        this.sampleKey = sampleKey;
    }

    public static IntegrationIcSampleRequest fromMap(Map<String, Object> body) {
        if (body == null) {
            return new IntegrationIcSampleRequest(DEFAULT_SAMPLE_KEY);
        }
        Object raw = body.get("sampleKey");
        String value = raw == null ? "" : String.valueOf(raw).trim();
        return new IntegrationIcSampleRequest(value.isEmpty() ? DEFAULT_SAMPLE_KEY : value);
    }

    public String getSampleKey() {
        return sampleKey;
    }
}
