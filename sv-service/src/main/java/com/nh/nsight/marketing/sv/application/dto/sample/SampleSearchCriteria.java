package com.nh.nsight.marketing.sv.application.dto.sample;

/**
 * SV 샘플 목록 조회 DAO/MyBatis 조건.
 */
public class SampleSearchCriteria {

    private final String sampleKey;
    private final int pageNo;
    private final int pageSize;
    private final int offset;

    public SampleSearchCriteria(String sampleKey, int pageNo, int pageSize, int offset) {
        this.sampleKey = sampleKey;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.offset = offset;
    }

    public String getSampleKey() {
        return sampleKey;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getOffset() {
        return offset;
    }
}
