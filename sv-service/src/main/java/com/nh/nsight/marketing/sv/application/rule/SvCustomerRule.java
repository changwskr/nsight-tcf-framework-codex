package com.nh.nsight.marketing.sv.application.rule;

import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryCriteria;
import com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryRequest;
import com.nh.nsight.marketing.sv.persistence.dto.customer.CustomerSummaryRow;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SvCustomerRule {
    private static final int MAX_CUSTOMER_NO_LENGTH = 20;

    public CustomerSummaryCriteria buildSummaryCriteria(CustomerSummaryRequest request) {
        if (request == null || !StringUtils.hasText(request.getCustomerNo())) {
            throw new BusinessException("E-SV-VAL-0001", "고객번호는 필수입니다.");
        }
        String customerNo = request.getCustomerNo();
        if (customerNo.length() > MAX_CUSTOMER_NO_LENGTH) {
            throw new BusinessException("E-SV-VAL-0002", "고객번호 길이가 올바르지 않습니다.");
        }
        String baseDate = request.getBaseDate();
        if (StringUtils.hasText(baseDate)) {
            return new CustomerSummaryCriteria(customerNo, baseDate);
        }
        return new CustomerSummaryCriteria(customerNo, null);
    }

    public void validateSummaryResult(CustomerSummaryRow customer) {
        if (customer == null || customer.isEmpty()) {
            throw new BusinessException("E-SV-BIZ-0001", "조회된 고객 정보가 없습니다.");
        }
    }
}
