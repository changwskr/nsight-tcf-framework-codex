package nhnis.mp.co.a.dto;

/**
 * mpcoa9999 전문 입력 DTO.
 *
 * <p>
 * 목록 조회(mpcoa9999S0_S0)는 salzTipKdc만 사용하고 비우면 전체를 조회한다.
 * 단건 조회(mpcoa9999S0_S1)는 네 항목 전부가 필수이며 TB_CR_AH_SALES_TIP_RACT_PK를 구성한다.
 */
public class mpcoa9999DtoIn {

    /** 취급점 코드 */
    private String trtBrc;

    /** 취급자 사번 */
    private String trtmnEno;

    /** 영업팁 종류코드 */
    private String salzTipKdc;

    /** 기준일자 (yyyyMMdd) */
    private String basDt;

    /** 페이징 번호 (1-based) */
    private Integer pageNo;

    /** 페이지 크기 */
    private Integer pageSize;

    /** 내부 페이징 오프셋 */
    private Integer offset;

    public String getTrtBrc() {
        return trtBrc;
    }

    public void setTrtBrc(String trtBrc) {
        this.trtBrc = trtBrc;
    }

    public String getTrtmnEno() {
        return trtmnEno;
    }

    public void setTrtmnEno(String trtmnEno) {
        this.trtmnEno = trtmnEno;
    }

    public String getSalzTipKdc() {
        return salzTipKdc;
    }

    public void setSalzTipKdc(String salzTipKdc) {
        this.salzTipKdc = salzTipKdc;
    }

    public String getBasDt() {
        return basDt;
    }

    public void setBasDt(String basDt) {
        this.basDt = basDt;
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

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}
