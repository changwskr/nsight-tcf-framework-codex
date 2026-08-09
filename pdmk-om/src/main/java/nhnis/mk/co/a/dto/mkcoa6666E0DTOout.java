package nhnis.mk.co.a.dto;

/**
 * sys_comm 거래통제 평가 결과 (mkcoa6666E0).
 * controlResult: ALLOW / REJECT / BLOCK / THROTTLE
 */
public class mkcoa6666E0DTOout {

    private String controlResult;
    private String errorCode;
    private String errorName;
    private String message;
    private String serviceCode;
    private String status;
    private Integer timeoutMs;
    private Integer maxTps;
    private Integer maxConcurrent;
    private Integer duplicateWindowSec;
    private String auditLevel;
    private Integer checkStep;

    public String getControlResult() { return controlResult; }
    public void setControlResult(String controlResult) { this.controlResult = controlResult; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorName() { return errorName; }
    public void setErrorName(String errorName) { this.errorName = errorName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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
    public Integer getCheckStep() { return checkStep; }
    public void setCheckStep(Integer checkStep) { this.checkStep = checkStep; }

    @Override
    public String toString() {
        return "controlResult=" + controlResult + " errorCode=" + errorCode
                + " serviceCode=" + serviceCode + " step=" + checkStep;
    }
}
