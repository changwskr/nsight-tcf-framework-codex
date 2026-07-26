package com.nh.nsight.tcf.eai.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 서비스 간 연동 호출 요청 모델.
 *
 * <p>업무 서비스가 다른 업무 서비스를 호출할 때 필요한 대상 식별 정보와 body를 담는다.
 * 실제 전문 header 는 {@code StandardRequestBuilder} 가 caller 컨텍스트와 결합하여 조립한다.
 */
public class IntegrationCallRequest {

    /** 대상 업무코드 (예: SV). URL·header.businessCode 조립에 사용. */
    private final String targetBusinessCode;

    /** 대상 serviceId (예: SV.Customer.selectSummary). Dispatcher 라우팅 키. */
    private final String targetServiceId;

    /** 대상 거래코드 (예: SV-INQ-0002). 거래통제/카탈로그 매핑용. */
    private final String targetTransactionCode;

    /** 거래명 (로그·전문 header 표기용, optional). */
    private final String serviceName;

    /** 처리 유형 (INQUIRY/EXECUTE). 미지정 시 INQUIRY. */
    private final String processingType;

    /** 전문 body. */
    private final Map<String, Object> body;

    private IntegrationCallRequest(Builder builder) {
        this.targetBusinessCode = builder.targetBusinessCode;
        this.targetServiceId = builder.targetServiceId;
        this.targetTransactionCode = builder.targetTransactionCode;
        this.serviceName = builder.serviceName;
        this.processingType = builder.processingType != null ? builder.processingType : "INQUIRY";
        this.body = builder.body != null ? builder.body : new LinkedHashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTargetBusinessCode() {
        return targetBusinessCode;
    }

    public String getTargetServiceId() {
        return targetServiceId;
    }

    public String getTargetTransactionCode() {
        return targetTransactionCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getProcessingType() {
        return processingType;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public static class Builder {
        private String targetBusinessCode;
        private String targetServiceId;
        private String targetTransactionCode;
        private String serviceName;
        private String processingType;
        private Map<String, Object> body;

        public Builder targetBusinessCode(String targetBusinessCode) {
            this.targetBusinessCode = targetBusinessCode;
            return this;
        }

        public Builder targetServiceId(String targetServiceId) {
            this.targetServiceId = targetServiceId;
            return this;
        }

        public Builder targetTransactionCode(String targetTransactionCode) {
            this.targetTransactionCode = targetTransactionCode;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder processingType(String processingType) {
            this.processingType = processingType;
            return this;
        }

        public Builder body(Map<String, Object> body) {
            this.body = body;
            return this;
        }

        public IntegrationCallRequest build() {
            return new IntegrationCallRequest(this);
        }
    }
}
