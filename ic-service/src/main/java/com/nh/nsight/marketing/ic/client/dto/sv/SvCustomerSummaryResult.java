package com.nh.nsight.marketing.ic.client.dto.sv;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SV.Customer.selectSummary 연동 응답 body (tcf-eai Map 파싱).
 */
public class SvCustomerSummaryResult {

    private final Map<String, Object> values;

    private SvCustomerSummaryResult(Map<String, Object> values) {
        this.values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static SvCustomerSummaryResult fromMap(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return new SvCustomerSummaryResult(Map.of());
        }
        return new SvCustomerSummaryResult(body);
    }

    public Object getCustomerGrade() {
        return values.get("customerGrade");
    }

    public Object getTotalBalance() {
        return values.get("totalBalance");
    }

    public Object getLastTransactionDate() {
        return values.get("lastTransactionDate");
    }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(values);
    }
}
