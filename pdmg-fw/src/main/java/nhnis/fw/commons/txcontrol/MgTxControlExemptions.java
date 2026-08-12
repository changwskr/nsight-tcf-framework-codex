package nhnis.fw.commons.txcontrol;

import java.util.Locale;

import org.springframework.util.StringUtils;

/**
 * 거래통제 검사 제외 serviceId (관리 CRUD·헬스 등).
 */
public final class MgTxControlExemptions {

    private MgTxControlExemptions() {
    }

    public static boolean isExempt(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return false;
        }
        String id = serviceId.trim();
        if (isTxControlAdmin(id)) {
            return true;
        }
        if (isRuntimeDiagnosis(id)) {
            return true;
        }
        if (isJwtAdmin(id)) {
            return true;
        }
        if (isHealth(id)) {
            return true;
        }
        return false;
    }

    /** mgcoa9001* — 거래통제 관리 자체는 차단하면 해제 불가 */
    public static boolean isTxControlAdmin(String serviceId) {
        return serviceId != null && serviceId.trim().startsWith("mgcoa9001");
    }

    /** mgcoa9100* — 런타임 진단 조회는 업무 통제에 막히면 안 됨 */
    public static boolean isRuntimeDiagnosis(String serviceId) {
        return serviceId != null && serviceId.trim().startsWith("mgcoa9100");
    }

    /** mgjwa* — 로그인/토큰 발급이 업무 통제에 막히면 복구 불가 */
    public static boolean isJwtAdmin(String serviceId) {
        return serviceId != null && serviceId.trim().toLowerCase(Locale.ROOT).startsWith("mgjwa");
    }

    private static boolean isHealth(String serviceId) {
        String id = serviceId.trim();
        return id.contains("HealthCheck")
                || id.endsWith(".healthCheck")
                || "health".equalsIgnoreCase(id);
    }
}
