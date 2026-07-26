package com.nh.nsight.marketing.sv.application.dto.integration;

import com.nh.nsight.marketing.sv.client.dto.ic.IcSampleInquiryResult;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SV.Integration.icSample 응답 body.
 */
public class IntegrationIcSampleResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final String sampleKey;
    private final IcSampleInquiryResult icSample;

    public IntegrationIcSampleResponse(
            String businessCode,
            String serviceId,
            String guid,
            String sampleKey,
            IcSampleInquiryResult icSample) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.sampleKey = sampleKey;
        this.icSample = icSample;
    }

    public static IntegrationIcSampleResponse of(
            TransactionContext context, String sampleKey, IcSampleInquiryResult icSample) {
        return new IntegrationIcSampleResponse(
                "SV",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                sampleKey,
                icSample);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.put("sampleKey", sampleKey);
        result.put("icSample", icSample == null ? Map.of() : icSample.toMap());
        return result;
    }
}
