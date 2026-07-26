package com.nh.nsight.marketing.sv.application.service;

import com.nh.nsight.marketing.sv.application.dto.integration.IntegrationIcSampleRequest;
import com.nh.nsight.marketing.sv.application.dto.integration.IntegrationIcSampleResponse;
import com.nh.nsight.marketing.sv.client.IcIntegrationClient;
import com.nh.nsight.marketing.sv.client.dto.ic.IcSampleInquiryResult;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.eai.exception.IntegrationBusinessException;
import com.nh.nsight.tcf.eai.exception.IntegrationException;
import com.nh.nsight.tcf.eai.exception.IntegrationTimeoutException;
import org.springframework.stereotype.Service;

/**
 * SV → IC 연동 데모 서비스.
 */
@Service
public class SvIntegrationDemoService {

    private final IcIntegrationClient icIntegrationClient;

    public SvIntegrationDemoService(IcIntegrationClient icIntegrationClient) {
        this.icIntegrationClient = icIntegrationClient;
    }

    public IntegrationIcSampleResponse inquiryIcSample(
            IntegrationIcSampleRequest request, TransactionContext context) {
        IcSampleInquiryResult icSample = callIcSample(request.getSampleKey(), context);
        return IntegrationIcSampleResponse.of(context, request.getSampleKey(), icSample);
    }

    private IcSampleInquiryResult callIcSample(String sampleKey, TransactionContext context) {
        try {
            return icIntegrationClient.inquirySample(sampleKey, context);
        } catch (IntegrationTimeoutException e) {
            throw new BusinessException(e.getErrorCode(), "IC 샘플 조회 응답 지연", e);
        } catch (IntegrationBusinessException e) {
            throw new BusinessException(e.getTargetResultCode(),
                    "IC 샘플 조회 업무 오류: " + e.getMessage(), e);
        } catch (IntegrationException e) {
            throw new BusinessException(e.getErrorCode(), "IC 샘플 조회 연동 오류", e);
        }
    }
}
