package com.nh.nsight.marketing.sv.application.rule;

import com.nh.nsight.marketing.sv.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.sv.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class SvSampleRule {
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 500;

    public void validateInquiry(SampleInquiryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 없습니다.");
        }
    }

    public SampleSearchCriteria buildSearchCriteria(SampleInquiryRequest request) {
        int pageNo = Math.max(1, toInt(request.getPageNo(), DEFAULT_PAGE_NO));
        int pageSize = toInt(request.getPageSize(), DEFAULT_PAGE_SIZE);
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        int offset = (pageNo - 1) * pageSize;
        String sampleKey = request.getSampleKey();
        if (sampleKey != null && sampleKey.isEmpty()) {
            sampleKey = null;
        }
        return new SampleSearchCriteria(sampleKey, pageNo, pageSize, offset);
    }

    private int toInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
