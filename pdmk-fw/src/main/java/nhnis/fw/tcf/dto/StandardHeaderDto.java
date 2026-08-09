package nhnis.fw.tcf.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * TCF 표준 전문 공통 헤더.
 *
 * <p>tcf-core의 {@code com.nh.nsight.tcf.core.support.message.StandardHeader}와 동일한 필드 구성이다.
 * pdmk-service는 tcf-core에 의존하지 않으므로 전문 호환을 위해 같은 구조를 유지한다.
 */
public class StandardHeaderDto implements Serializable {

    /** 연계 시스템 ID. 미입력 시 normalize()가 기본값을 채운다. */
    private String systemId;

    /** 업무 코드 (CC, SV, OM ...). normalize()가 대문자로 정규화한다. */
    private String businessCode;

    /** 거래 식별자. 예: SV.Sample.inquiry */
    private String serviceId;

    /** 거래명 */
    private String serviceName;

    /** 화면·거래 코드. 예: SV-INQ-0001 */
    private String transactionCode;

    /** 처리 유형. {@link ProcessingType} 이름 문자열을 사용한다. */
    private String processingType;

    /** 거래 GUID. 빈 값이면 프레임워크가 생성한다. */
    private String guid;

    /** 추적 ID. 빈 값이면 프레임워크가 생성한다. */
    private String traceId;

    /** 채널 ID. 예: WEBTOP, OM-PORTAL */
    private String channelId;

    /** 사용자 ID */
    @JsonAlias("user")
    private String userId;

    /** 부점 코드 */
    @JsonAlias("branch")
    private String branchId;

    /** 센터 코드 */
    private String centerId;

    /** 요청 시각 (ISO-8601). 미입력 시 normalize()가 현재 시각을 채운다. */
    private String requestTime;

    /** 클라이언트 IP */
    private String clientIp;

    /** 멱등 키 */
    private String idempotencyKey;

    /** 기본값 보완과 대문자 정규화. */
    public void normalize() {
        if (systemId == null || systemId.isBlank()) {
            systemId = DEFAULT_SYSTEM_ID;
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

    /** systemId 미입력 시 채우는 기본 시스템 ID. */
    public static final String DEFAULT_SYSTEM_ID = "NSIGHT-MP";

    /** 서버 처리 전 클라이언트 Header 값 보존용 복사본. */
    public static StandardHeaderDto copyOf(StandardHeaderDto source) {
        if (source == null) {
            return null;
        }
        StandardHeaderDto copy = new StandardHeaderDto();
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
     * 나머지 필드는 클라이언트 원본을 유지한다.
     */
    public void applyGeneratedCorrelationIdsFrom(StandardHeaderDto processed) {
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

    /** 로그용 serviceId. 미설정이면 UNKNOWN. */
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
