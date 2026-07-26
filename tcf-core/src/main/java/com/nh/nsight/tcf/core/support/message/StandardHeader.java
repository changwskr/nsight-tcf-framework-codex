package com.nh.nsight.tcf.core.support.message;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class StandardHeader implements Serializable {
    private String systemId;
    private String businessCode;
    private String serviceId;
    private String serviceName;
    private String transactionCode;
    private String processingType;
    private String guid;
    private String traceId;
    private String channelId;
    @JsonAlias("user")
    private String userId;
    @JsonAlias("branch")
    private String branchId;
    private String centerId;
    private String requestTime;
    private String clientIp;
    private String idempotencyKey;

    public void normalize() {
        if (systemId == null || systemId.isBlank()) {
            systemId = "NSIGHT-MP";
        }
        if (requestTime == null || requestTime.isBlank()) {
            requestTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        if (businessCode != null) {
            businessCode = businessCode.trim().toUpperCase();
        }
        if (processingType != null) {
            processingType = processingType.trim().toUpperCase();
        }
    }

    /** STF 처리 전 클라이언트 Header 값 보존용 복사본. */
    public static StandardHeader copyOf(StandardHeader source) {
        if (source == null) {
            return null;
        }
        StandardHeader copy = new StandardHeader();
        copy.setSystemId(source.getSystemId());
        copy.setBusinessCode(source.getBusinessCode());
        copy.setServiceId(source.getServiceId());
        copy.setServiceName(source.getServiceName());
        copy.setTransactionCode(source.getTransactionCode());
        copy.setProcessingType(source.getProcessingType());
        copy.setGuid(source.getGuid());
        copy.setTraceId(source.getTraceId());
        copy.setChannelId(source.getChannelId());
        copy.setUserId(source.getUserId());
        copy.setBranchId(source.getBranchId());
        copy.setCenterId(source.getCenterId());
        copy.setRequestTime(source.getRequestTime());
        copy.setClientIp(source.getClientIp());
        copy.setIdempotencyKey(source.getIdempotencyKey());
        return copy;
    }

    /**
     * 클라이언트가 보내지 않은 guid/traceId만 서버 처리값으로 보완한다.
     * normalize 등 기타 필드는 클라이언트 원본을 유지한다.
     */
    public void applyGeneratedCorrelationIdsFrom(StandardHeader processed) {
        if (processed == null) {
            return;
        }
        if ((guid == null || guid.isBlank()) && processed.getGuid() != null && !processed.getGuid().isBlank()) {
            guid = processed.getGuid();
        }
        if ((traceId == null || traceId.isBlank()) && processed.getTraceId() != null && !processed.getTraceId().isBlank()) {
            traceId = processed.getTraceId();
        }
    }

    public String safeServiceId() {
        return Objects.toString(serviceId, "UNKNOWN");
    }

    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }
    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public String getProcessingType() { return processingType; }
    public void setProcessingType(String processingType) { this.processingType = processingType; }
    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getCenterId() { return centerId; }
    public void setCenterId(String centerId) { this.centerId = centerId; }
    public String getRequestTime() { return requestTime; }
    public void setRequestTime(String requestTime) { this.requestTime = requestTime; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
