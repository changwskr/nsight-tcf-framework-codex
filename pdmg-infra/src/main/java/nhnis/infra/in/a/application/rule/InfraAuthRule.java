package nhnis.infra.in.a.application.rule;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.persistence.dao.ifina1500DAO;

/**
 * 화면 §34.13 RACI — person.ROLE_CD(OPEN-02 파일럿) + 정적 폴백.
 * <p>mode=off|soft|hard. hard 시 RSLT_CD=0006.
 * <p>Phase3: SEC=GATE5만, 기준정보 Admin만, DBA/MW 본인영역,
 * Lifecycle 점프는 {@link LifecycleTransitionRule}.
 */
@Component
public class InfraAuthRule {
    public enum Mode { OFF, SOFT, HARD }

    private static final Set<String> DB_TECH_ROLES = Set.of("DATABASE", "DB", "RDBMS");
    private static final Set<String> MW_TECH_ROLES = Set.of(
            "WAS", "WEB", "APP", "API_GW", "MW", "MQ", "CACHE", "INTEGRATION");

    private final Mode mode;
    private final String defaultRole;
    private final Map<String, String> roleByOptr;
    private final ifina1500DAO personDao;

    @org.springframework.beans.factory.annotation.Autowired
    public InfraAuthRule(
            ifina1500DAO personDao,
            @Value("${infra.auth.raci.mode:soft}") String mode,
            @Value("${infra.auth.raci.default-role:ARCH}") String defaultRole) {
        this.personDao = personDao;
        this.mode = parse(mode);
        this.defaultRole = defaultRole == null || defaultRole.isBlank()
                ? "ARCH" : defaultRole.trim().toUpperCase(Locale.ROOT);
        this.roleByOptr = Map.of(
                "E0000001", "ARCH",
                "E0000002", "OPS",
                "E0000003", "SEC",
                "E0000004", "PMO",
                "E0000005", "ADMIN",
                "E0000006", "DBA",
                "E0000007", "MW",
                "LOCAL", this.defaultRole);
    }

    public static InfraAuthRule forUnitTest(String mode, String defaultRole) {
        return new InfraAuthRule(null, mode, defaultRole);
    }

    public Mode getMode() {
        return mode;
    }

    public ValidationResult evaluate(String serviceId) {
        return evaluate(serviceId, null);
    }

    /** @param attrs 예: gateId (SEC GATE5 한정) */
    public ValidationResult evaluate(String serviceId, Map<String, String> attrs) {
        ValidationResult r = new ValidationResult();
        if (mode == Mode.OFF || serviceId == null || serviceId.isBlank()) {
            return r;
        }
        String role = resolveRole();
        String sid = serviceId.trim();
        if (needsGateJudge(sid)) {
            if (!Set.of("ARCH", "ADMIN", "SEC").contains(role)) {
                add(r, "RL-AU-003", "Gate 판정은 Arch/Admin/Security 역할 필요 (role=" + role + ")");
            } else if ("SEC".equals(role)) {
                String gateId = attrs == null ? null : attrs.get("gateId");
                if (gateId != null && !gateId.isBlank()
                        && !"GATE5".equalsIgnoreCase(gateId.trim())) {
                    add(r, "RL-AU-003",
                            "Security는 GATE5만 판정 가능 (gate=" + gateId.trim().toUpperCase(Locale.ROOT)
                                    + ", role=" + role + ")");
                }
            }
        }
        // Phase3: 기준정보는 Admin만 (ARCH는 △ → soft/hard 경고·거절)
        if (needsMasterEdit(sid) && !Set.of("ADMIN").contains(role)) {
            add(r, "RL-AU-003", "기준정보 마스터 변경은 Admin 필요 (role=" + role + ")");
        }
        if (needsSecurityEdit(sid) && !Set.of("ARCH", "ADMIN", "SEC").contains(role)) {
            add(r, "RL-AU-001", "보안 프로파일 편집은 Arch/Admin/Security 필요 (role=" + role + ")");
        }
        if (needsCostOrMigration(sid) && !Set.of("ARCH", "PMO", "ADMIN").contains(role)) {
            add(r, "RL-AU-001", "비용/라이선스/전환 편집은 Arch/PMO 필요 (role=" + role + ")");
        }
        if (needsInventoryWrite(sid)) {
            if ("PMO".equals(role)) {
                add(r, "RL-AU-001", "PMO는 인벤토리·기술영역 편집 제한 (role=" + role + ")");
            } else if (("DBA".equals(role) || "MW".equals(role))
                    && !isDomainWriteAllowed(role, sid, attrs)) {
                add(r, "RL-AU-001",
                        "DBA/MW는 본인영역만 편집 가능 (role=" + role + ", service=" + sid + ")");
            }
        }
        return r;
    }

    public String resolveRole() {
        ServiceContext ctx = ServiceContextHolder.getInstance();
        String optr = null;
        if (ctx != null && ctx.getHeader() != null && ctx.getHeader().getSys_comm() != null) {
            optr = ctx.getHeader().getSys_comm().getOptr_eno();
        }
        if (optr == null || optr.isBlank()) {
            return roleByOptr.getOrDefault("LOCAL", defaultRole);
        }
        String key = optr.trim();
        String fromDb = lookupDbRole(key);
        if (fromDb != null && !fromDb.isBlank()) {
            return fromDb.trim().toUpperCase(Locale.ROOT);
        }
        return roleByOptr.getOrDefault(key, roleByOptr.getOrDefault("LOCAL", defaultRole));
    }

    private String lookupDbRole(String personId) {
        if (personDao == null) {
            return null;
        }
        try {
            return personDao.ifina1500S0_roleByPersonId(Map.of("personId", personId));
        } catch (Exception e) {
            return null;
        }
    }

    private void add(ValidationResult r, String ruleId, String msg) {
        if (mode == Mode.HARD) {
            r.add(RuleViolation.hard(ruleId, "0006", msg));
        } else {
            r.add(RuleViolation.soft(ruleId, msg));
        }
    }

    private static boolean needsGateJudge(String sid) {
        return sid.startsWith("ifina9200U0");
    }

    private static boolean needsMasterEdit(String sid) {
        return sid.startsWith("ifina1100C") || sid.startsWith("ifina1100U")
                || sid.startsWith("ifina1200C") || sid.startsWith("ifina1200U") || sid.startsWith("ifina1200D")
                || sid.startsWith("ifina1300C") || sid.startsWith("ifina1300U")
                || sid.startsWith("ifina1400C") || sid.startsWith("ifina1400U")
                || sid.startsWith("ifina1500C") || sid.startsWith("ifina1500U") || sid.startsWith("ifina1500E");
    }

    private static boolean needsSecurityEdit(String sid) {
        return sid.startsWith("ifina6300U");
    }

    private static boolean needsCostOrMigration(String sid) {
        return sid.startsWith("ifina7100C") || sid.startsWith("ifina7100U") || sid.startsWith("ifina7100D")
                || sid.startsWith("ifina7200U")
                || sid.startsWith("ifina7300C")
                || sid.startsWith("ifina8100C") || sid.startsWith("ifina8100U") || sid.startsWith("ifina8100D")
                || sid.startsWith("ifina8200C") || sid.startsWith("ifina8200U");
    }

    private static boolean needsInventoryWrite(String sid) {
        return sid.matches(
                "ifina(1999|2100|2200|2300|3100|3110|3400|4100|4200|5100|5200|5300|6100|6200|9100)[CUD]0");
    }

    /**
     * DBA: DB 인스턴스(ifina4200) + RDBMS 자산(ifina3100+techRole).
     * MW: MW 설치(ifina4100) + WAS/WEB 등 자산(ifina3100+techRole).
     * Checklist(ifina9100)은 양쪽 허용.
     */
    static boolean isDomainWriteAllowed(String role, String sid, Map<String, String> attrs) {
        if (sid.startsWith("ifina9100")) {
            return true;
        }
        if ("DBA".equals(role)) {
            if (sid.startsWith("ifina4200")) {
                return true;
            }
            if (sid.startsWith("ifina3100")) {
                return isDbTechRole(attr(attrs, "techRoleCd"));
            }
            return false;
        }
        if ("MW".equals(role)) {
            if (sid.startsWith("ifina4100")) {
                return true;
            }
            if (sid.startsWith("ifina3100")) {
                return isMwTechRole(attr(attrs, "techRoleCd"));
            }
            return false;
        }
        return true;
    }

    private static boolean isDbTechRole(String techRoleCd) {
        if (techRoleCd == null || techRoleCd.isBlank()) {
            return false;
        }
        return DB_TECH_ROLES.contains(techRoleCd.trim().toUpperCase(Locale.ROOT));
    }

    private static boolean isMwTechRole(String techRoleCd) {
        if (techRoleCd == null || techRoleCd.isBlank()) {
            return false;
        }
        return MW_TECH_ROLES.contains(techRoleCd.trim().toUpperCase(Locale.ROOT));
    }

    private static String attr(Map<String, String> attrs, String key) {
        return attrs == null ? null : attrs.get(key);
    }

    private static Mode parse(String mode) {
        if (mode == null || mode.isBlank()) {
            return Mode.SOFT;
        }
        return switch (mode.trim().toLowerCase(Locale.ROOT)) {
            case "hard" -> Mode.HARD;
            case "off", "false", "none" -> Mode.OFF;
            default -> Mode.SOFT;
        };
    }
}
