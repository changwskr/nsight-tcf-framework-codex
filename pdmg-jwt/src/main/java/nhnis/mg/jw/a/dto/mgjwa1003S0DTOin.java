package nhnis.mg.jw.a.dto;

/**
 * Refresh Token 조회 입력 (mgjwa1003S0).
 */
public class mgjwa1003S0DTOin {

    private Integer pageNo;
    private Integer pageSize;
    private String userId;
    private String revokedYn;
    private String activeOnly;

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRevokedYn() {
        return revokedYn;
    }

    public void setRevokedYn(String revokedYn) {
        this.revokedYn = revokedYn;
    }

    public String getActiveOnly() {
        return activeOnly;
    }

    public void setActiveOnly(String activeOnly) {
        this.activeOnly = activeOnly;
    }
}
