package nhnis.infra.in.a.application.support;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OPEN-02 IdP 역할 → RACI ROLE_CD 매핑.
 * <p>실제 IdP 커넥터 대신 배치/웹훅 동기화 API({@code ifina1500E0})가 사용한다.
 */
@Component
@ConfigurationProperties(prefix = "infra.auth.idp")
public class IdpAuthProperties {
    private static final Set<String> RACI_ROLES = Set.of(
            "ARCH", "OPS", "SEC", "PMO", "ADMIN", "DBA", "MW");

    private boolean enabled = true;
    private boolean createMissing = true;
    private String defaultOrgId = "ORG-INFRA";
    private Map<String, String> roleMap = defaultRoleMap();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCreateMissing() {
        return createMissing;
    }

    public void setCreateMissing(boolean createMissing) {
        this.createMissing = createMissing;
    }

    public String getDefaultOrgId() {
        return defaultOrgId;
    }

    public void setDefaultOrgId(String defaultOrgId) {
        this.defaultOrgId = defaultOrgId;
    }

    public Map<String, String> getRoleMap() {
        return roleMap;
    }

    public void setRoleMap(Map<String, String> roleMap) {
        this.roleMap = roleMap != null ? roleMap : defaultRoleMap();
    }

    /**
     * IdP 클레임/그룹명을 RACI ROLE_CD로 변환. 미매핑 시 null.
     */
    public String resolveRaciRole(String idpRole) {
        if (idpRole == null || idpRole.isBlank()) {
            return null;
        }
        String raw = idpRole.trim();
        String upper = raw.toUpperCase(Locale.ROOT);
        if (RACI_ROLES.contains(upper)) {
            return upper;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        String mapped = roleMap.get(key);
        if (mapped == null) {
            mapped = roleMap.get(raw);
        }
        if (mapped == null) {
            return null;
        }
        String role = mapped.trim().toUpperCase(Locale.ROOT);
        return RACI_ROLES.contains(role) ? role : null;
    }

    private static Map<String, String> defaultRoleMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("infra-arch", "ARCH");
        m.put("role-arch", "ARCH");
        m.put("infra-ops", "OPS");
        m.put("role-ops", "OPS");
        m.put("security-arch", "SEC");
        m.put("role-sec", "SEC");
        m.put("pmo", "PMO");
        m.put("role-pmo", "PMO");
        m.put("infra-admin", "ADMIN");
        m.put("role-admin", "ADMIN");
        m.put("dba", "DBA");
        m.put("role-dba", "DBA");
        m.put("middleware", "MW");
        m.put("role-mw", "MW");
        return m;
    }
}
