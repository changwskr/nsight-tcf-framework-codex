package com.nh.nsight.marketing.av.application.rule;

import com.nh.nsight.marketing.av.application.dto.sample.SampleInquiryRequest;
import com.nh.nsight.marketing.av.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AvSampleRule {
    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int MAX_PAGE_SIZE = 100;

    public void validateInquiry(SampleInquiryRequest request) {
        if (request == null) {
            return;
        }
        int pageSize = request.getPageSize() != null ? request.getPageSize() : DEFAULT_PAGE_SIZE;
        if (pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "pageSize는 최대 " + MAX_PAGE_SIZE + " 입니다.");
        }
        int pageNo = request.getPageNo() != null ? request.getPageNo() : 1;
        if (pageNo < 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "pageNo는 1 이상이어야 합니다.");
        }
    }

    public SampleSearchCriteria buildSearchCriteria(SampleInquiryRequest request) {
        SampleInquiryRequest safe = request != null ? request : SampleInquiryRequest.fromMap(Map.of());
        int pageNo = safe.getPageNo() != null ? safe.getPageNo() : 1;
        int pageSize = safe.getPageSize() != null ? safe.getPageSize() : DEFAULT_PAGE_SIZE;
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setOffset((pageNo - 1) * pageSize);
        criteria.setSampleKey(safe.getSampleKey());
        criteria.setSampleName(safe.getSampleName());
        criteria.setUseYn(safe.getUseYn());
        return criteria;
    }
}
