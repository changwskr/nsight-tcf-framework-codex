package nhnis.mg.co.a.dto;

/** 런타임 진단 조회 입력 (mgcoa9100S0). */
public class mgcoa9100S0DTOin {

    /** Y 이면 활성 거래 등 상세 포함 */
    private String includeDetails = "Y";

    public String getIncludeDetails() {
        return includeDetails;
    }

    public void setIncludeDetails(String includeDetails) {
        this.includeDetails = includeDetails;
    }
}
