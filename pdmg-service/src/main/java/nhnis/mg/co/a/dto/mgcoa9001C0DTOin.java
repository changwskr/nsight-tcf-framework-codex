package nhnis.mg.co.a.dto;

/** 거래통제 등록 입력 (mgcoa9001C0). OM TransactionControl.save 대응. */
public class mgcoa9001C0DTOin {

    private String controlType;
    private String targetValue;
    private String blockYn;
    private String changeReason;
    private String regUserId;

    public String getControlType() { return controlType; }
    public void setControlType(String controlType) { this.controlType = controlType; }
    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }
    public String getBlockYn() { return blockYn; }
    public void setBlockYn(String blockYn) { this.blockYn = blockYn; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public String getRegUserId() { return regUserId; }
    public void setRegUserId(String regUserId) { this.regUserId = regUserId; }
}
