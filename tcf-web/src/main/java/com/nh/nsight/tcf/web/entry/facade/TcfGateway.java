package com.nh.nsight.tcf.web.entry.facade;

import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.message.StandardResponse;
import com.nh.nsight.tcf.core.support.processor.TCF;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * REST·파일 API 등 비표준 진입점에서도 동일한 TCF 파이프라인(STF → Dispatcher → ETF)을 거치도록 위임합니다.
 */
@Component
public class TcfGateway {
    private final TCF tcf;

    public TcfGateway(TCF tcf) {
        this.tcf = tcf;
    }

    public StandardResponse<Object> invoke(TcfInvokeRequest invokeRequest) {
        StandardRequest<Map<String, Object>> request = new StandardRequest<>(
                buildHeader(invokeRequest),
                invokeRequest.body() == null ? Map.of() : invokeRequest.body()
        );
        return tcf.process(request);
    }

    public static StandardHeader buildHeader(TcfInvokeRequest invokeRequest) {
        StandardHeader header = new StandardHeader();
        header.setSystemId("NSIGHT-MP");
        header.setBusinessCode(invokeRequest.businessCode());
        header.setServiceId(invokeRequest.serviceId());
        header.setTransactionCode(invokeRequest.transactionCode());
        header.setProcessingType(invokeRequest.processingType());
        header.setChannelId(StringUtils.hasText(invokeRequest.channelId()) ? invokeRequest.channelId() : "WEBTOP");
        if (StringUtils.hasText(invokeRequest.userId())) {
            header.setUserId(invokeRequest.userId());
        }
        if (StringUtils.hasText(invokeRequest.clientIp())) {
            header.setClientIp(invokeRequest.clientIp());
        }
        return header;
    }

    public record TcfInvokeRequest(
            String businessCode,
            String serviceId,
            String transactionCode,
            String processingType,
            Map<String, Object> body,
            String userId,
            String clientIp,
            String channelId
    ) {
        public static Builder builder(String serviceId, String transactionCode, String processingType) {
            String businessCode = serviceId != null && serviceId.contains(".")
                    ? serviceId.substring(0, serviceId.indexOf('.'))
                    : "UD";
            return new Builder(businessCode, serviceId, transactionCode, processingType);
        }

        public static final class Builder {
            private final String businessCode;
            private final String serviceId;
            private final String transactionCode;
            private final String processingType;
            private Map<String, Object> body = Map.of();
            private String userId;
            private String clientIp;
            private String channelId;

            private Builder(String businessCode, String serviceId, String transactionCode, String processingType) {
                this.businessCode = businessCode;
                this.serviceId = serviceId;
                this.transactionCode = transactionCode;
                this.processingType = processingType;
            }

            public Builder body(Map<String, Object> body) {
                this.body = body;
                return this;
            }

            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public Builder clientIp(String clientIp) {
                this.clientIp = clientIp;
                return this;
            }

            public Builder channelId(String channelId) {
                this.channelId = channelId;
                return this;
            }

            public TcfInvokeRequest build() {
                return new TcfInvokeRequest(
                        businessCode, serviceId, transactionCode, processingType, body, userId, clientIp, channelId
                );
            }
        }
    }
}
