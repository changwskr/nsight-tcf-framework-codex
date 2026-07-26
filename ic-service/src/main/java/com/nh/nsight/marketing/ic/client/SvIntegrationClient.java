package com.nh.nsight.marketing.ic.client;

import com.nh.nsight.marketing.ic.client.dto.sv.SvCustomerSummaryResult;
import com.nh.nsight.tcf.eai.client.TcfServiceClient;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * SV(Single View) 서비스 연동 Adapter.
 */
@Component
public class SvIntegrationClient {

    private static final String TARGET_BUSINESS_CODE = "SV";
    private static final String SVC_CUSTOMER_SUMMARY = "SV.Customer.selectSummary";
    private static final String TX_CUSTOMER_SUMMARY = "SV-INQ-0002";

    private final TcfServiceClient tcfServiceClient;

    public SvIntegrationClient(TcfServiceClient tcfServiceClient) {
        this.tcfServiceClient = tcfServiceClient;
    }

    public SvCustomerSummaryResult selectCustomerSummary(
            String customerNo,
            com.nh.nsight.tcf.core.support.context.TransactionContext callerContext) {
        Map<String, Object> body = tcfServiceClient.callForBody(
                TARGET_BUSINESS_CODE,
                SVC_CUSTOMER_SUMMARY,
                TX_CUSTOMER_SUMMARY,
                Map.of("customerNo", customerNo),
                callerContext);
        return SvCustomerSummaryResult.fromMap(body);
    }
}
