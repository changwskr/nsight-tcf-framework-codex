package nhnis.fw.tcf;

/**
 * log4j2.xml의 헤더 패턴이 참조하는 MDC 키.
 *
 * <p>필터·STF·ETF가 모두 참조하므로 특정 계층에 두면 패키지 순환이 생긴다. 그래서 중립 위치에 둔다.
 */
public final class TcfMdcKeys {

    public static final String GUID = "guid";
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String SERVICE_ID = "serviceId";
    public static final String IP = "ip";
    public static final String ERR_CODE = "errCode";

    private TcfMdcKeys() {
    }
}
