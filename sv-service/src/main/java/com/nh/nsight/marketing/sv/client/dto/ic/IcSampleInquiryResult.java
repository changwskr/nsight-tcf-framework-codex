package com.nh.nsight.marketing.sv.client.dto.ic;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IC.Sample.inquiry 연동 응답 body (tcf-eai Map 파싱).
 */
public class IcSampleInquiryResult {

    private final Map<String, Object> values;

    private IcSampleInquiryResult(Map<String, Object> values) {
        this.values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static IcSampleInquiryResult fromMap(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return new IcSampleInquiryResult(Map.of());
        }
        return new IcSampleInquiryResult(body);
    }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
