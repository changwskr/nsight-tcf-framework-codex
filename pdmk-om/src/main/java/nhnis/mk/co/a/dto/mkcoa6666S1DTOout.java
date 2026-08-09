package nhnis.mk.co.a.dto;

/**
 * 서비스별 거래통제 상세 (mkcoa6666S1).
 * 02.거래통제UI화면.md 기본정보 + 정책 + 런타임 요약.
 */
public class mkcoa6666S1DTOout extends mkcoa6666S0DTOSub0 {

    private Integer currentTps;
    private Integer currentConcurrent;
    private Integer recentBlockCount;
    private String onlineForceState;

    public Integer getCurrentTps() { return currentTps; }
    public void setCurrentTps(Integer currentTps) { this.currentTps = currentTps; }
    public Integer getCurrentConcurrent() { return currentConcurrent; }
    public void setCurrentConcurrent(Integer currentConcurrent) { this.currentConcurrent = currentConcurrent; }
    public Integer getRecentBlockCount() { return recentBlockCount; }
    public void setRecentBlockCount(Integer recentBlockCount) { this.recentBlockCount = recentBlockCount; }
    public String getOnlineForceState() { return onlineForceState; }
    public void setOnlineForceState(String onlineForceState) { this.onlineForceState = onlineForceState; }
}
