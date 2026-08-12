package nhnis.mg.jw.a.dto;

import java.util.List;
import java.util.Map;

/**
 * 토큰 현황 조회 결과 (mgjwa1001S0).
 *
 * <p>{@code rows} 항목은 DAO 조회 결과(Map)를 그대로 담아 pdmg-ui 가 참조하는
 * 컬럼명(jti, userId, userName, channelId, issuedAt, expiresAt, revokedYn, clientIp 등)을 그대로 유지한다.
 */
public class mgjwa1001S0DTOout {

    private String businessCode;
    private String screen;
    private Integer pageNo;
    private Integer pageSize;
    private Long totalCount;
    private List<Map<String, Object>> rows;

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }

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

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }
}
