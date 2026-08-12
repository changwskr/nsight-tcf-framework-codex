package nhnis.fw.commons.log;

/**
 * PDMG 운영 트랜잭션 로그 메시지 포맷.
 *
 * <p>실제 {@code log.info} 호출은 호출부(Interceptor/Aspect/Controller/Service)에서 해야
 * Log4j {@code %C.%M} 위치가 맞는다. 이 클래스는 메시지 문자열만 만든다.
 *
 * <pre>
 * (라인피드 5개)
 * [시스템 선처리] 시작 ===============================================
 * [시스템 선처리] GUID: ...
 * [온라인전문][요청][원문] ===============================================
 * [요청전문] { 클라이언트 요청 전문 원문 }
 * [시스템 선처리] 종료 ===============================================
 *
 * [업무 선처리] 시작 ===============================================
 * (진행 로그)
 * [업무 선처리] 종료 ===============================================
 *
 * [업무처리] 시작 ===============================================
 * → Controller / Service / DAO
 * [업무처리] 종료 ===============================================
 *
 * [업무 후처리] 시작 ===============================================
 * (결과 DTO)
 * [업무 후처리] 종료 ===============================================
 *
 * [시스템 후처리] 시작 ===============================================
 * [온라인전문][응답][원문] ===============================================
 * { 클라이언트 응답 전문 원문 }
 * [시스템 후처리] 종료 ===============================================
 * </pre>
 */
public final class PdmgTxLog {

    /** Controller Start/End (8) */
    public static final String CONTROLLER = "▷▷▷▷▷▷▷▷";

    /** Service Start/End (8) */
    public static final String SERVICE = "▶▶▶▶▶▶▶▶";

    /** 구간 구분선 (통일) */
    private static final String LINE = "===============================================";

    private PdmgTxLog() {
    }

    /** 거래 구분용 라인피드 5개. */
    public static String txGap() {
        return "\n\n\n\n\n";
    }

    private static String section(String name, String phase) {
        return "[" + name + "] " + phase + " " + LINE;
    }

    public static String systemPreStart() {
        return section("시스템 선처리", "시작");
    }

    public static String systemPreEnd() {
        return section("시스템 선처리", "종료");
    }

    public static String systemPostStart() {
        return section("시스템 후처리", "시작");
    }

    public static String systemPostEnd() {
        return section("시스템 후처리", "종료");
    }

    public static String systemGuid(String guid) {
        return "[시스템 선처리] GUID: " + guid;
    }

    public static String systemErrorProcessor() {
        return "[시스템 후처리] 오류 " + LINE;
    }

    public static String systemContextNull() {
        return "[시스템 선처리] Service Context is null...!! (continue)";
    }

    public static String systemRequestMessage() {
        return "[시스템 선처리] 클라이언트 온라인 전문";
    }

    public static String systemResponseMessage() {
        return "[시스템 후처리] 응답 전문";
    }

    /** 시스템 선처리 이후 — 클라이언트 요청 온라인 전문 원문. */
    public static String onlineRequestAsIs() {
        return "[온라인전문][요청][원문] " + LINE;
    }

    /** 시스템 후처리 이후 — 클라이언트로 내려가는 응답 온라인 전문 원문. */
    public static String onlineResponseAsIs() {
        return "[온라인전문][응답][원문] " + LINE;
    }

    public static String bizPreStart() {
        return section("업무 선처리", "시작");
    }

    public static String bizPreEnd() {
        return section("업무 선처리", "종료");
    }

    public static String bizPreProgress(String message) {
        return "[업무 선처리] " + message;
    }

    public static String bizProcessStart() {
        return section("업무처리", "시작");
    }

    public static String bizProcessEnd() {
        return section("업무처리", "종료");
    }

    public static String bizPostStart() {
        return section("업무 후처리", "시작");
    }

    public static String bizPostEnd() {
        return section("업무 후처리", "종료");
    }

    public static String bizResponseMessage() {
        return "[업무 후처리] 결과 DTO";
    }

    public static String controllerStart(String programId) {
        return CONTROLLER + " " + programId + " Controller Start!";
    }

    public static String controllerEnd(String programId, String methodName, Object dto) {
        return CONTROLLER + " " + programId + " Controller End!" + methodName + "DTOsub0 : " + dto;
    }

    public static String serviceStart(String methodName) {
        return SERVICE + " " + methodName + " Service Start!";
    }

    public static String serviceEnd(String methodName, Object total) {
        return SERVICE + " " + methodName + " Service End! - Total: " + total;
    }
}
