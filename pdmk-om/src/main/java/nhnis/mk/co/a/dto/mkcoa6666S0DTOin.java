package nhnis.mk.co.a.dto;

/**
 * Service Catalog 조회/등록/수정 공통 입력 (mkcoa6666).
 * 요건 §26 거래통제 기준정보 속성.
 */
public class mkcoa6666S0DTOin {

    private String serviceCode;
    private String serviceName;
    private String businessCode;
    private String scid;
    private String enabled;
    private String status;
    private String onlineForceYn;
    private String allowedSystemIds;
    private String allowedTerminalTypes;
    private String allowedBranches;
    private String requiredAuthorities;
    private String syncType;
    private String allowedStartTime;
    private String allowedEndTime;
    private Integer timeoutMs;
    private Integer maxTps;
    private Integer maxConcurrent;
    private Integer duplicateWindowSec;
    private String auditLevel;
    private String reason;
    private String policyJson;
    private Integer pageNo;
    private Integer pageSize;

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getScid() { return scid; }
    public void setScid(String scid) { this.scid = scid; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOnlineForceYn() { return onlineForceYn; }
    public void setOnlineForceYn(String onlineForceYn) { this.onlineForceYn = onlineForceYn; }
    public String getAllowedSystemIds() { return allowedSystemIds; }
    public void setAllowedSystemIds(String allowedSystemIds) { this.allowedSystemIds = allowedSystemIds; }
    public String getAllowedTerminalTypes() { return allowedTerminalTypes; }
    public void setAllowedTerminalTypes(String allowedTerminalTypes) { this.allowedTerminalTypes = allowedTerminalTypes; }
    public String getAllowedBranches() { return allowedBranches; }
    public void setAllowedBranches(String allowedBranches) { this.allowedBranches = allowedBranches; }
    public String getRequiredAuthorities() { return requiredAuthorities; }
    public void setRequiredAuthorities(String requiredAuthorities) { this.requiredAuthorities = requiredAuthorities; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getAllowedStartTime() { return allowedStartTime; }
    public void setAllowedStartTime(String allowedStartTime) { this.allowedStartTime = allowedStartTime; }
    public String getAllowedEndTime() { return allowedEndTime; }
    public void setAllowedEndTime(String allowedEndTime) { this.allowedEndTime = allowedEndTime; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Integer getMaxTps() { return maxTps; }
    public void setMaxTps(Integer maxTps) { this.maxTps = maxTps; }
    public Integer getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(Integer maxConcurrent) { this.maxConcurrent = maxConcurrent; }
    public Integer getDuplicateWindowSec() { return duplicateWindowSec; }
    public void setDuplicateWindowSec(Integer duplicateWindowSec) { this.duplicateWindowSec = duplicateWindowSec; }
    public String getAuditLevel() { return auditLevel; }
    public void setAuditLevel(String auditLevel) { this.auditLevel = auditLevel; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPolicyJson() { return policyJson; }
    public void setPolicyJson(String policyJson) { this.policyJson = policyJson; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
