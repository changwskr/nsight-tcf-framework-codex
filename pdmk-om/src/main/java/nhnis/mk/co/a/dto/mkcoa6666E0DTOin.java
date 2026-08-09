package nhnis.mk.co.a.dto;

/**
 * sys_comm 거래통제 평가 입력 (mkcoa6666E0).
 * 필드명은 sys_comm 과 대응 (camelCase).
 */
public class mkcoa6666E0DTOin {

    private String stdGblId;
    private String rmsSvcC;
    private String syncDsc;
    private String trSysid;
    private Integer ttlUgYnc;
    private String stdTgrmRqrRspDsc;
    private String stdTgrmLclc;
    private String trTrmIpadr;
    private String trDtm;
    private String trBrc;
    private String trmno;
    private String trmKdc;
    private String scid;
    private String optrEno;
    private String trOptrnm;
    /** 서버 인증 Context 사용자 (있으면 optr_eno 정합 검증) */
    private String authUserId;

    public String getStdGblId() { return stdGblId; }
    public void setStdGblId(String stdGblId) { this.stdGblId = stdGblId; }
    public String getRmsSvcC() { return rmsSvcC; }
    public void setRmsSvcC(String rmsSvcC) { this.rmsSvcC = rmsSvcC; }
    public String getSyncDsc() { return syncDsc; }
    public void setSyncDsc(String syncDsc) { this.syncDsc = syncDsc; }
    public String getTrSysid() { return trSysid; }
    public void setTrSysid(String trSysid) { this.trSysid = trSysid; }
    public Integer getTtlUgYnc() { return ttlUgYnc; }
    public void setTtlUgYnc(Integer ttlUgYnc) { this.ttlUgYnc = ttlUgYnc; }
    public String getStdTgrmRqrRspDsc() { return stdTgrmRqrRspDsc; }
    public void setStdTgrmRqrRspDsc(String stdTgrmRqrRspDsc) { this.stdTgrmRqrRspDsc = stdTgrmRqrRspDsc; }
    public String getStdTgrmLclc() { return stdTgrmLclc; }
    public void setStdTgrmLclc(String stdTgrmLclc) { this.stdTgrmLclc = stdTgrmLclc; }
    public String getTrTrmIpadr() { return trTrmIpadr; }
    public void setTrTrmIpadr(String trTrmIpadr) { this.trTrmIpadr = trTrmIpadr; }
    public String getTrDtm() { return trDtm; }
    public void setTrDtm(String trDtm) { this.trDtm = trDtm; }
    public String getTrBrc() { return trBrc; }
    public void setTrBrc(String trBrc) { this.trBrc = trBrc; }
    public String getTrmno() { return trmno; }
    public void setTrmno(String trmno) { this.trmno = trmno; }
    public String getTrmKdc() { return trmKdc; }
    public void setTrmKdc(String trmKdc) { this.trmKdc = trmKdc; }
    public String getScid() { return scid; }
    public void setScid(String scid) { this.scid = scid; }
    public String getOptrEno() { return optrEno; }
    public void setOptrEno(String optrEno) { this.optrEno = optrEno; }
    public String getTrOptrnm() { return trOptrnm; }
    public void setTrOptrnm(String trOptrnm) { this.trOptrnm = trOptrnm; }
    public String getAuthUserId() { return authUserId; }
    public void setAuthUserId(String authUserId) { this.authUserId = authUserId; }
}
