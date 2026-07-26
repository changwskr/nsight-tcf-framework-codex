package com.nh.nsight.marketing.ic.application.dto.sample;

/**
 * IC 샘플 조회 DAO 조건.
 */
public class SampleSearchCriteria {

    private final String sampleKey;

    public SampleSearchCriteria(String sampleKey) {
        this.sampleKey = sampleKey;
    }

    public String getSampleKey() {
        return sampleKey;
    }
}
