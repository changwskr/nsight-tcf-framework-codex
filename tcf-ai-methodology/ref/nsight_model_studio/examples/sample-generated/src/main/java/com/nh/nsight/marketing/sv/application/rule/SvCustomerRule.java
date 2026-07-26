package com.nh.nsight.marketing.sv.application.rule;

import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryCriteria;
import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryRequest;
import com.nh.nsight.marketing.sv.persistence.dto.customer.CustomerSummaryRow;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SvCustomerRule {

    public CustomerSummaryCriteria buildCustomerSummaryCriteria(CustomerSummaryRequest request) {
        if (request == null) {
            throw new BusinessException("E-SV-VAL-0000", "요청 정보가 없습니다.");
        }
        if (!StringUtils.hasText(request.getCustomerNo())) {
            throw new BusinessException("E-SV-VAL-0001", "고객번호은(는) 필수입니다.");
        }
        if (StringUtils.hasText(request.getCustomerNo()) && request.getCustomerNo().length() > 20) {
            throw new BusinessException("E-SV-VAL-0002", "고객번호 길이가 20자를 초과했습니다.");
        }
        return new CustomerSummaryCriteria(request.getCustomerNo());
    }

    public void validateCustomerSummaryResult(CustomerSummaryRow result) {
        if (result == null || result.isEmpty()) {
            throw new BusinessException("E-SV-BIZ-0001", "조회된 고객 종합정보 조회 정보가 없습니다.");
        }
    }
}
