package com.nh.nsight.marketing.ic.application.dto.customer;

import com.nh.nsight.marketing.ic.client.dto.sv.SvCustomerSummaryResult;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IC.Customer.inquiry 응답 body.
 */
public class CustomerInquiryResponse {

    private final String businessCode;
    private final String serviceId;
    private final String guid;
    private final String customerNo;
    private final String customerName;
    private final Object customerGrade;
    private final Object totalBalance;
    private final Object lastTransactionDate;
    private final SvCustomerSummaryResult svSummary;

    public CustomerInquiryResponse(
            String businessCode,
            String serviceId,
            String guid,
            String customerNo,
            String customerName,
            Object customerGrade,
            Object totalBalance,
            Object lastTransactionDate,
            SvCustomerSummaryResult svSummary) {
        this.businessCode = businessCode;
        this.serviceId = serviceId;
        this.guid = guid;
        this.customerNo = customerNo;
        this.customerName = customerName;
        this.customerGrade = customerGrade;
        this.totalBalance = totalBalance;
        this.lastTransactionDate = lastTransactionDate;
        this.svSummary = svSummary;
    }

    public static CustomerInquiryResponse of(
            TransactionContext context,
            String customerNo,
            String customerName,
            SvCustomerSummaryResult svSummary) {
        return new CustomerInquiryResponse(
                "IC",
                context.getHeader().getServiceId(),
                context.getHeader().getGuid(),
                customerNo,
                customerName,
                svSummary.getCustomerGrade(),
                svSummary.getTotalBalance(),
                svSummary.getLastTransactionDate(),
                svSummary);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", businessCode);
        result.put("serviceId", serviceId);
        result.put("guid", guid);
        result.put("customerNo", customerNo);
        result.put("customerName", customerName);
        result.put("customerGrade", customerGrade);
        result.put("totalBalance", totalBalance);
        result.put("lastTransactionDate", lastTransactionDate);
        result.put("svSummary", svSummary == null ? Map.of() : svSummary.toMap());
        return result;
    }
}
