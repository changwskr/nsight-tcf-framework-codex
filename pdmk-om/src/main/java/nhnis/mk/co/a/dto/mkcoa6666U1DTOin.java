package nhnis.mk.co.a.dto;

/**
 * 상태 전용 변경 (mkcoa6666U1) — 중지/점검/재개.
 */
public class mkcoa6666U1DTOin {

    private String serviceCode;
    private String status;
    private String enabled;
    private String onlineForceYn;
    private String reason;

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getOnlineForceYn() { return onlineForceYn; }
    public void setOnlineForceYn(String onlineForceYn) { this.onlineForceYn = onlineForceYn; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
