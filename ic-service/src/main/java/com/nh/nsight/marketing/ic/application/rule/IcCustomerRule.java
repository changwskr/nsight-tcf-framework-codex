package com.nh.nsight.marketing.ic.application.rule;

import com.nh.nsight.marketing.ic.application.dto.customer.CustomerInquiryRequest;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * IC 고객 조회 업무 규칙. 순수 입력 검증만 담당(외부 호출 금지).
 */
@Component
public class IcCustomerRule {

    public String validateInquiry(CustomerInquiryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 비어 있습니다.");
        }
        if (!StringUtils.hasText(request.getCustomerNo())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "customerNo는 필수입니다.");
        }
        return request.getCustomerNo();
    }
}
