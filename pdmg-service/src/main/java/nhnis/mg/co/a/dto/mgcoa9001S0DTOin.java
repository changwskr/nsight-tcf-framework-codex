package nhnis.mg.co.a.dto;

/** 거래통제 목록 조회 입력 (mgcoa9001S0). OM TransactionControl.inquiry 대응. */
public class mgcoa9001S0DTOin {

    private String controlType;
    private String targetValue;
    private String blockYn;
    private Integer pageNo;
    private Integer pageSize;

    public String getControlType() { return controlType; }
    public void setControlType(String controlType) { this.controlType = controlType; }
    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }
    public String getBlockYn() { return blockYn; }
    public void setBlockYn(String blockYn) { this.blockYn = blockYn; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
