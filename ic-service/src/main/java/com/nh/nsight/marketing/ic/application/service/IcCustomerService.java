package com.nh.nsight.marketing.ic.application.service;

import com.nh.nsight.marketing.ic.application.dto.customer.CustomerInquiryRequest;
import com.nh.nsight.marketing.ic.application.dto.customer.CustomerInquiryResponse;
import com.nh.nsight.marketing.ic.application.rule.IcCustomerRule;
import com.nh.nsight.marketing.ic.client.SvIntegrationClient;
import com.nh.nsight.marketing.ic.client.dto.sv.SvCustomerSummaryResult;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.eai.exception.IntegrationBusinessException;
import com.nh.nsight.tcf.eai.exception.IntegrationException;
import com.nh.nsight.tcf.eai.exception.IntegrationTimeoutException;
import org.springframework.stereotype.Service;

/**
 * IC 고객 상세 조회 서비스.
 */
@Service
public class IcCustomerService {

    private final IcCustomerRule rule;
    private final SvIntegrationClient svIntegrationClient;

    public IcCustomerService(IcCustomerRule rule, SvIntegrationClient svIntegrationClient) {
        this.rule = rule;
        this.svIntegrationClient = svIntegrationClient;
    }

    public CustomerInquiryResponse inquiryCustomerDetail(
            CustomerInquiryRequest request, TransactionContext context) {
        String customerNo = rule.validateInquiry(request);
        String customerName = "IC-" + customerNo;
        SvCustomerSummaryResult svSummary = callSvSummary(customerNo, context);
        return CustomerInquiryResponse.of(context, customerNo, customerName, svSummary);
    }

    private SvCustomerSummaryResult callSvSummary(String customerNo, TransactionContext context) {
        try {
            return svIntegrationClient.selectCustomerSummary(customerNo, context);
        } catch (IntegrationTimeoutException e) {
            throw new BusinessException(e.getErrorCode(), "SV 고객요약 조회 응답 지연", e);
        } catch (IntegrationBusinessException e) {
            throw new BusinessException(e.getTargetResultCode(),
                    "SV 고객요약 조회 업무 오류: " + e.getMessage(), e);
        } catch (IntegrationException e) {
            throw new BusinessException(e.getErrorCode(), "SV 고객요약 조회 연동 오류", e);
        }
    }
}
