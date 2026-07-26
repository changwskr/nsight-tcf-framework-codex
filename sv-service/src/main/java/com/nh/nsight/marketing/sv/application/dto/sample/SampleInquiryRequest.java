package com.nh.nsight.marketing.sv.application.dto.sample;

import java.util.Map;

/**
 * SV.Sample.inquiry 요청 body.
 */
public class SampleInquiryRequest {

    private final String sampleKey;
    private final Integer pageNo;
    private final Integer pageSize;

    public SampleInquiryRequest(String sampleKey, Integer pageNo, Integer pageSize) {
        this.sampleKey = sampleKey;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public static SampleInquiryRequest fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return new SampleInquiryRequest(
                stringValue(body.get("sampleKey")),
                toInteger(body.get("pageNo")),
                toInteger(body.get("pageSize")));
    }

    public String getSampleKey() {
        return sampleKey;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
