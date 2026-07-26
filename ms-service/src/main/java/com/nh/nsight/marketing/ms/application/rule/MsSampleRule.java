package com.nh.nsight.marketing.ms.application.rule;

import com.nh.nsight.marketing.ms.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.ms.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class MsSampleRule {
    public void validateInquiry(SampleInquiryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 비어 있습니다.");
        }
    }
    public SampleSearchCriteria buildSearchCriteria(SampleInquiryRequest request) {
        String sampleKey = request.getSampleKey() == null ? "SAMPLE" : request.getSampleKey();
        return new SampleSearchCriteria(sampleKey);
    }
}
