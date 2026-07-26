package com.nh.nsight.marketing.eb.application.dto.systemtx;

import java.util.Map;

/**
 * 화면 19410 시스템 거래 현황 조회 요청.
 */
public class SystemTxInquiryRequest {

    private final Integer pageNo;
    private final Integer pageSize;
    private final String txDateFrom;
    private final String txDateTo;
    private final String txType;
    private final String txSeqNo;
    private final String empNo;
    private final String screenId;
    private final String serviceId;

    public SystemTxInquiryRequest(
            Integer pageNo,
            Integer pageSize,
            String txDateFrom,
            String txDateTo,
            String txType,
            String txSeqNo,
            String empNo,
            String screenId,
            String serviceId) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.txDateFrom = txDateFrom;
        this.txDateTo = txDateTo;
        this.txType = txType;
        this.txSeqNo = txSeqNo;
        this.empNo = empNo;
        this.screenId = screenId;
        this.serviceId = serviceId;
    }

    public static SystemTxInquiryRequest fromMap(Map<String, Object> body) {
        Map<String, Object> safe = body != null ? body : Map.of();
        return new SystemTxInquiryRequest(
                toInteger(safe.get("pageNo")),
                toInteger(safe.get("pageSize")),
                trimToNull(safe.get("txDateFrom")),
                trimToNull(safe.get("txDateTo")),
                trimToNull(safe.get("txType")),
                trimToNull(safe.get("txSeqNo")),
                trimToNull(safe.get("empNo")),
                trimToNull(firstNonBlank(safe, "screenId", "screenNo")),
                trimToNull(safe.get("serviceId")));
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public String getTxDateFrom() {
        return txDateFrom;
    }

    public String getTxDateTo() {
        return txDateTo;
    }

    public String getTxType() {
        return txType;
    }

    public String getTxSeqNo() {
        return txSeqNo;
    }

    public String getEmpNo() {
        return empNo;
    }

    public String getScreenId() {
        return screenId;
    }

    public String getServiceId() {
        return serviceId;
    }

    private static Object firstNonBlank(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
