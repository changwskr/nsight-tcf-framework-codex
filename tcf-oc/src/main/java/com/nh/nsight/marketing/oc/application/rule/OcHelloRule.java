package com.nh.nsight.marketing.oc.application.rule;

import com.nh.nsight.marketing.oc.application.dto.hello.HelloInquiryRequest;
import com.nh.nsight.marketing.oc.application.dto.hello.HelloSearchCriteria;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class OcHelloRule {

    public void validateInquiry(HelloInquiryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 비어 있습니다.");
        }
    }

    public HelloSearchCriteria buildSearchCriteria(HelloInquiryRequest request) {
        String name = request.getName() == null ? "World" : request.getName();
        return new HelloSearchCriteria(name);
    }
}
