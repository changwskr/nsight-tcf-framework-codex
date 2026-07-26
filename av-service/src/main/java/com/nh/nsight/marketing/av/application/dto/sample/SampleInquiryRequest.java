package com.nh.nsight.marketing.av.application.dto.sample;

import java.util.Map;

public class SampleInquiryRequest {

    private final Integer pageNo;
    private final Integer pageSize;
    private final String sampleKey;
    private final String sampleName;
    private final String useYn;

    public SampleInquiryRequest(Integer pageNo, Integer pageSize, String sampleKey, String sampleName, String useYn) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.sampleKey = sampleKey;
        this.sampleName = sampleName;
        this.useYn = useYn;
    }

    public static SampleInquiryRequest fromMap(Map<String, Object> body) {
        Map<String, Object> safeBody = body != null ? body : Map.of();
        return new SampleInquiryRequest(
                toInteger(safeBody.get("pageNo")),
                toInteger(safeBody.get("pageSize")),
                trimToNull(safeBody.get("sampleKey")),
                trimToNull(safeBody.get("sampleName")),
                trimToNull(safeBody.get("useYn")));
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public String getSampleKey() {
        return sampleKey;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getUseYn() {
        return useYn;
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

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
