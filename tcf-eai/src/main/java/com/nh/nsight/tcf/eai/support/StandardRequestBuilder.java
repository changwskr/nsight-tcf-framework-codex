package com.nh.nsight.tcf.eai.support;

import com.nh.nsight.tcf.eai.model.IntegrationCallRequest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 연동 호출 요청 + 전파 정보를 NSIGHT 표준 전문(Map: header/body)으로 조립한다.
 */
public final class StandardRequestBuilder {

    private StandardRequestBuilder() {
    }

    public static Map<String, Object> build(IntegrationCallRequest request,
                                            HeaderPropagationHelper.Propagation propagation) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("header", buildHeader(request, propagation));
        message.put("body", request.getBody());
        return message;
    }

    private static Map<String, Object> buildHeader(IntegrationCallRequest request,
                                                   HeaderPropagationHelper.Propagation propagation) {
        OffsetDateTime now = OffsetDateTime.now();
        String systemDate = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("systemId", "NSIGHT-MP");
        header.put("businessCode", request.getTargetBusinessCode());
        header.put("serviceId", request.getTargetServiceId());
        // serviceName 은 거래통제 필수 항목(E-TCF-HDR-004). 미지정 시 serviceId 로 보완.
        header.put("serviceName", firstNonBlank(request.getServiceName(), request.getTargetServiceId()));
        header.put("transactionCode", request.getTargetTransactionCode());
        header.put("processingType", request.getProcessingType());

        // 상관관계 전파: 원 거래 GUID/TraceId 유지
        header.put("guid", propagation.getGuid());
        header.put("traceId", propagation.getTraceId());

        // 호출자 컨텍스트 전파
        header.put("channelId", firstNonBlank(propagation.getChannelId(), propagation.getCallerBusinessCode()));
        header.put("userId", firstNonBlank(propagation.getUserId(), "SYSTEM"));
        header.put("branchId", firstNonBlank(propagation.getBranchId(), "000"));
        header.put("callerBusinessCode", propagation.getCallerBusinessCode());
        header.put("callerServiceId", propagation.getCallerServiceId());

        header.put("requestTime", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        header.put("systemDate", systemDate);
        header.put("bizDate", systemDate);
        return header;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
