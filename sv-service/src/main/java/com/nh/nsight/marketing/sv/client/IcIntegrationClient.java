package com.nh.nsight.marketing.sv.client;

import com.nh.nsight.marketing.sv.client.dto.ic.IcSampleInquiryResult;
import com.nh.nsight.tcf.eai.client.TcfServiceClient;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * IC(Integration Customer) 서비스 연동 Adapter (SV → IC 방향 데모).
 */
@Component
public class IcIntegrationClient {

    private static final String TARGET_BUSINESS_CODE = "IC";
    private static final String SVC_SAMPLE_INQUIRY = "IC.Sample.inquiry";
    private static final String TX_SAMPLE_INQUIRY = "IC-INQ-0001";

    private final TcfServiceClient tcfServiceClient;

    public IcIntegrationClient(TcfServiceClient tcfServiceClient) {
        this.tcfServiceClient = tcfServiceClient;
    }

    public IcSampleInquiryResult inquirySample(
            String sampleKey,
            com.nh.nsight.tcf.core.support.context.TransactionContext callerContext) {
        Map<String, Object> body = tcfServiceClient.callForBody(
                TARGET_BUSINESS_CODE,
                SVC_SAMPLE_INQUIRY,
                TX_SAMPLE_INQUIRY,
                Map.of("sampleKey", sampleKey),
                callerContext);
        return IcSampleInquiryResult.fromMap(body);
    }
}
