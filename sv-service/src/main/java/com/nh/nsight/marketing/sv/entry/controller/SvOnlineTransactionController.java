package com.nh.nsight.marketing.sv.entry.controller;

import com.nh.nsight.marketing.sv.entry.facade.SvSampleFacade;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.message.StandardResponse;
import com.nh.nsight.tcf.util.GuidGenerator;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TCF({@code POST /online})를 거치지 않고 외부 시스템이 SV 업무 Facade에 직접 진입하는 샘플 Controller.
 *
 * <p>STF/ETF 전처리·후처리, 거래통제, 멱등성 검증 등 TCF 파이프라인은 수행하지 않습니다.
 * 업무 로직은 기존 Facade → Service 체인을 그대로 사용합니다.
 */
@RestController
@RequestMapping("/sv/direct")
public class SvOnlineTransactionController {

    private static final String SAMPLE_INQUIRY = "SV.Sample.inquiry";

    private final SvSampleFacade sampleFacade;

    public SvOnlineTransactionController(SvSampleFacade sampleFacade) {
        this.sampleFacade = sampleFacade;
    }

    @GetMapping
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "SV 외부 직접 인입 샘플 (TCF bypass)");
        body.put("endpoint", "POST /sv/direct/online");
        body.put("supportedServiceIds", new String[]{SAMPLE_INQUIRY});
        body.put("requestExample", Map.of(
                "serviceId", SAMPLE_INQUIRY,
                "userId", "U123456",
                "channelId", "PARTNER-API",
                "body", Map.of("pageNo", 1, "pageSize", 10, "sampleKey", "A00")));
        return body;
    }

    @PostMapping("/online")
    public StandardResponse<Object> handleDirect(
            @RequestBody DirectOnlineRequest request,
            HttpServletRequest servletRequest) {
        if (request == null || !StringUtils.hasText(request.getServiceId())) {
            throw new BusinessException(ErrorCode.INVALID_HEADER, "serviceId는 필수입니다.");
        }
        TransactionContext context = buildContext(request, servletRequest);
        Object resultBody = dispatch(request.getServiceId().trim(), request.getBody(), context);
        return StandardResponse.success(context.getClientHeader(), resultBody);
    }

    private Object dispatch(String serviceId, Map<String, Object> body, TransactionContext context) {
        Map<String, Object> safeBody = body != null ? body : Map.of();
        return switch (serviceId) {
            case SAMPLE_INQUIRY -> sampleFacade.inquiry(safeBody, context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "SvOnlineTransactionController 미지원 serviceId: " + serviceId);
        };
    }

    private TransactionContext buildContext(DirectOnlineRequest request, HttpServletRequest servletRequest) {
        StandardHeader header = new StandardHeader();
        header.setSystemId("NSIGHT-MP");
        header.setBusinessCode("SV");
        header.setServiceId(request.getServiceId().trim());
        header.setServiceName("SV 외부 직접 인입");
        header.setTransactionCode("SV-DIR-0001");
        header.setProcessingType("INQUIRY");
        header.setGuid(GuidGenerator.newGuid());
        header.setTraceId(GuidGenerator.newTraceId());
        header.setChannelId(StringUtils.hasText(request.getChannelId()) ? request.getChannelId() : "EXTERNAL");
        header.setUserId(StringUtils.hasText(request.getUserId()) ? request.getUserId() : "EXTERNAL");
        header.setBranchId(StringUtils.hasText(request.getBranchId()) ? request.getBranchId() : "000000");
        header.setRequestTime(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        header.setClientIp(resolveClientIp(servletRequest));
        header.normalize();
        return new TransactionContext(header);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 외부 직접 인입용 간소 요청 전문. 표준 {@code StandardRequest} header/body 래핑 없이 사용합니다.
     */
    public static class DirectOnlineRequest {

        private String serviceId;
        private String userId;
        private String channelId;
        private String branchId;
        private Map<String, Object> body;

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getChannelId() {
            return channelId;
        }

        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }

        public String getBranchId() {
            return branchId;
        }

        public void setBranchId(String branchId) {
            this.branchId = branchId;
        }

        public Map<String, Object> getBody() {
            return body;
        }

        public void setBody(Map<String, Object> body) {
            this.body = body;
        }
    }
}
