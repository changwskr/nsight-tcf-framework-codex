package nhnis.mp.co.a.dto;

/**
 * mpcoa9999 전문 출력 DTO. TB_CR_AH_SALES_TIP_RACT 한 행에 대응한다.
 *
 * <p>매퍼 XML에서 컬럼을 이 필드명으로 alias 하므로 MyBatis가 자동 매핑한다.
 */
public class mpcoa9999DtoOut {

    /** 취급점 코드 */
    private String trtBrc;

    /** 취급자 사번 */
    private String trtmnEno;

    /** 영업팁 종류코드 */
    private String salzTipKdc;

    /** 기준일자 (yyyyMMdd) */
    private String basDt;

    /** 포트폴리오 내용 */
    private String prtoCn;

    /** 조회 내용 */
    private String inqCn;

    /** 입력 내용 */
    private String inpCn;

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

    public String getPrtoCn() {
        return prtoCn;
    }

    public void setPrtoCn(String prtoCn) {
        this.prtoCn = prtoCn;
    }

    public String getInqCn() {
        return inqCn;
    }

    public void setInqCn(String inqCn) {
        this.inqCn = inqCn;
    }

    public String getInpCn() {
        return inpCn;
    }

    public void setInpCn(String inpCn) {
        this.inpCn = inpCn;
    }
}
