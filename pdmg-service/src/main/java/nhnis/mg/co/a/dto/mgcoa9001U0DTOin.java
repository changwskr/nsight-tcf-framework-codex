package nhnis.mg.co.a.dto;

/** 거래통제 수정 입력 (mgcoa9001U0). OM TransactionControl.update 대응. */
public class mgcoa9001U0DTOin {

    private String serviceId;
    private String transactionCode;
    private String businessCode;
    private String serviceName;
    private String userId;
    private String channelId;
    private String branchId;
    private String controlType;
    private String targetValue;
    private String blockYn;
    private String changeReason;
    private String chgUserId;

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getControlType() { return controlType; }
    public void setControlType(String controlType) { this.controlType = controlType; }
    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }
    public String getBlockYn() { return blockYn; }
    public void setBlockYn(String blockYn) { this.blockYn = blockYn; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public String getChgUserId() { return chgUserId; }
    public void setChgUserId(String chgUserId) { this.chgUserId = chgUserId; }
}
