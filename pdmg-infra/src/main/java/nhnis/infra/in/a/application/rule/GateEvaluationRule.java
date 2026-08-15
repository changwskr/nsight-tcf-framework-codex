package nhnis.infra.in.a.application.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import nhnis.infra.in.a.application.service.ifina6200Service;
import nhnis.infra.in.a.application.service.ifina6300Service;
import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.persistence.dao.ifina6100DAO;
import nhnis.infra.in.a.persistence.dao.ifina6200DAO;
import nhnis.infra.in.a.persistence.dao.ifina6300DAO;
import nhnis.infra.in.a.persistence.dao.ifina7300DAO;
import nhnis.infra.in.a.persistence.dao.ifina9100DAO;
import nhnis.infra.in.a.persistence.dao.ifinaAuditDAO;

/**
 * Gate 판정 전 RL-GT-* / RL-AV-* / RL-CP-* / RL-SC-* / Gate6 비용 평가.
 */
@Component
public class GateEvaluationRule {
    private final ifina9100DAO checklistDao;
    private final ifina6100DAO availDao;
    private final ifina6200DAO capacityDao;
    private final ifina6300DAO securityDao;
    private final ifinaAuditDAO auditDao;
    private final ifina7300DAO costDao;

    public GateEvaluationRule(
            ifina9100DAO checklistDao,
            ifina6100DAO availDao,
            ifina6200DAO capacityDao,
            ifina6300DAO securityDao,
            ifinaAuditDAO auditDao,
            ifina7300DAO costDao) {
        this.checklistDao = checklistDao;
        this.availDao = availDao;
        this.capacityDao = capacityDao;
        this.securityDao = securityDao;
        this.auditDao = auditDao;
        this.costDao = costDao;
    }

    public ValidationResult evaluate(
            String gateId, String targetTypeCd, String targetId, String resultCd, String evidence)
            throws Exception {
        ValidationResult r = new ValidationResult();
        String gate = safe(gateId).toUpperCase(Locale.ROOT);
        String result = safe(resultCd).toUpperCase(Locale.ROOT);
        if (!List.of("PASS", "CONDITIONAL", "FAIL").contains(result)) {
            r.add(RuleViolation.hard("RL-GT-004", "0001", "result_cd는 PASS/CONDITIONAL/FAIL"));
            return r;
        }
        if ("PASS".equals(result)) {
            if ("GATE1".equals(gate)) {
                validateChecklistComplete(r, targetTypeCd, targetId);
            }
            if ("GATE3".equals(gate)) {
                validateGate3Capacity(r, targetTypeCd, targetId);
            }
            if ("GATE5".equals(gate)) {
                validateGate5Ha(r, targetTypeCd, targetId);
                validateGate5Security(r, targetTypeCd, targetId);
            }
            if ("GATE6".equals(gate)) {
                validateGate6Cost(r, targetTypeCd, targetId);
            }
        } else {
            if (isBlank(evidence) && !hasEvidence(targetTypeCd, targetId, gate)) {
                r.add(RuleViolation.hard("RL-GT-003", "0001", "FAIL/CONDITIONAL 시 Evidence 필수"));
            }
        }
        return r;
    }

    public List<String> softHints(String gateId, String targetTypeCd, String targetId) throws Exception {
        List<String> hints = new ArrayList<>();
        String gate = safe(gateId).toUpperCase(Locale.ROOT);
        if ("GATE3".equals(gate) || gate.isEmpty()) {
            hints.addAll(capacitySoftHints(targetTypeCd, targetId));
        }
        if ("GATE5".equals(gate) || gate.isEmpty()) {
            Map<String, Object> p = availDao.ifina6100S0_S1(Map.of(
                    "targetTypeCd", blank(targetTypeCd, "GROUP"),
                    "targetId", safe(targetId)));
            if (p == null || p.isEmpty()) {
                hints.add("[RL-AV-001] HA profile 없음");
            } else {
                if (!"Y".equalsIgnoreCase(as(p, "HA_YN", "haYn"))) {
                    hints.add("[RL-AV-001] Tier1/대상 HA_YN=N");
                }
                if (as(p, "RTO_MINUTES", "rtoMinutes") == null || as(p, "RPO_MINUTES", "rpoMinutes") == null) {
                    hints.add("[RL-AV-002] RTO/RPO 미정");
                }
            }
            Map<String, Object> sec = securityDao.ifina6300S0_S1(Map.of(
                    "targetTypeCd", blank(targetTypeCd, "GROUP"),
                    "targetId", safe(targetId)));
            if (sec == null || sec.isEmpty()) {
                hints.add("[RL-SC-001] security_profile 없음");
            } else {
                hints.addAll(ifina6300Service.evaluateSoftMap(toSecurityMap(sec)));
            }
        }
        if ("GATE6".equals(gate) || gate.isEmpty()) {
            CostKey key = resolveCostKey(targetTypeCd, targetId);
            int cnt = costDao.ifina7300S0_countTarget(Map.of(
                    "targetTypeCd", key.targetTypeCd(),
                    "targetId", key.targetId()));
            if (cnt <= 0) {
                hints.add("[RL-GT-006] cost_snapshot 없음 (" + key.targetTypeCd() + "/" + key.targetId() + ")");
            }
        }
        return hints;
    }

    private void validateChecklistComplete(ValidationResult r, String targetType, String targetId) throws Exception {
        List<Map<String, Object>> rows = checklistDao.ifina9100S0_S0(Map.of(
                "targetType", blank(targetType, "ASSET"),
                "targetId", safe(targetId)));
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<String> open = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (!"Y".equalsIgnoreCase(as(row, "CHECKED_YN", "checkedYn"))) {
                open.add(as(row, "CHECKLIST_ID", "checklistId"));
            }
        }
        if (!open.isEmpty()) {
            r.add(RuleViolation.hard("RL-GT-001", "0001",
                    "Checklist 미완료: " + String.join(",", open)));
        }
    }

    private void validateGate3Capacity(ValidationResult r, String targetType, String targetId) throws Exception {
        List<Map<String, Object>> mapped = loadCapacityRows(targetType, targetId);
        ValidationResult soft = ifina6200Service.evaluate(mapped);
        for (RuleViolation v : soft.getViolations()) {
            r.add(RuleViolation.hard(v.getRuleId(), "0001", "GATE3: " + v.getMessage()));
        }
    }

    private void validateGate5Ha(ValidationResult r, String targetType, String targetId) throws Exception {
        Map<String, Object> p = availDao.ifina6100S0_S1(Map.of(
                "targetTypeCd", blank(targetType, "GROUP"),
                "targetId", safe(targetId)));
        if (p == null || p.isEmpty()) {
            r.add(RuleViolation.hard("RL-GT-002", "0001", "GATE5: HA profile 없음 (RL-AV-001)"));
            return;
        }
        if (!"Y".equalsIgnoreCase(as(p, "HA_YN", "haYn"))) {
            r.add(RuleViolation.hard("RL-GT-002", "0001", "GATE5: HA_YN=Y 필요 (RL-AV-001)"));
        }
        if (as(p, "RTO_MINUTES", "rtoMinutes") == null || as(p, "RPO_MINUTES", "rpoMinutes") == null) {
            r.add(RuleViolation.hard("RL-GT-002", "0001", "GATE5: RTO/RPO 필요 (RL-AV-002)"));
        }
    }

    private void validateGate5Security(ValidationResult r, String targetType, String targetId) throws Exception {
        Map<String, Object> p = securityDao.ifina6300S0_S1(Map.of(
                "targetTypeCd", blank(targetType, "GROUP"),
                "targetId", safe(targetId)));
        if (p == null || p.isEmpty()) {
            r.add(RuleViolation.hard("RL-SC-001", "0001", "GATE5: security_profile 필요"));
        }
    }

    private void validateGate6Cost(ValidationResult r, String targetType, String targetId) throws Exception {
        CostKey key = resolveCostKey(targetType, targetId);
        int cnt = costDao.ifina7300S0_countTarget(Map.of(
                "targetTypeCd", key.targetTypeCd(),
                "targetId", key.targetId()));
        if (cnt <= 0) {
            r.add(RuleViolation.hard("RL-GT-006", "0001",
                    "GATE6: cost_snapshot 필요 (" + key.targetTypeCd() + "/" + key.targetId() + ")"));
        }
    }

    private List<String> capacitySoftHints(String targetType, String targetId) throws Exception {
        return ifina6200Service.evaluate(loadCapacityRows(targetType, targetId)).softWarnings();
    }

    private List<Map<String, Object>> loadCapacityRows(String targetType, String targetId) throws Exception {
        List<Map<String, Object>> raw = capacityDao.ifina6200S0_S0(Map.of(
                "targetTypeCd", blank(targetType, "GROUP"),
                "targetId", safe(targetId)));
        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                rows.add(mapCapacity(row));
            }
        }
        return rows;
    }

    private static Map<String, Object> mapCapacity(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("metricScopeCd", as(row, "METRIC_SCOPE_CD", "metricScopeCd"));
        Object tps = row.get("TPS");
        if (tps == null) {
            tps = row.get("tps");
        }
        m.put("tps", tps == null ? null : toBd(tps));
        return m;
    }

    private static Map<String, Object> toSecurityMap(Map<String, Object> row) {
        Map<String, Object> p = new HashMap<>();
        p.put("personalInfoYn", as(row, "PERSONAL_INFO_YN", "personalInfoYn"));
        p.put("creditInfoYn", as(row, "CREDIT_INFO_YN", "creditInfoYn"));
        p.put("encryptionYn", as(row, "ENCRYPTION_YN", "encryptionYn"));
        p.put("pamYn", as(row, "PAM_YN", "pamYn"));
        p.put("adminInfoYn", as(row, "ADMIN_INFO_YN", "adminInfoYn"));
        p.put("externalConnYn", as(row, "EXTERNAL_CONN_YN", "externalConnYn"));
        p.put("auditLogYn", as(row, "AUDIT_LOG_YN", "auditLogYn"));
        p.put("networkZoneCd", as(row, "NETWORK_ZONE_CD", "networkZoneCd"));
        return p;
    }

    /**
     * 파일럿: SYSTEM 대상은 그대로, 그 외는 SYS-ONLINE 비용으로 판정.
     */
    private static CostKey resolveCostKey(String targetType, String targetId) {
        if ("SYSTEM".equalsIgnoreCase(safe(targetType))) {
            return new CostKey("SYSTEM", blank(targetId, "SYS-ONLINE"));
        }
        return new CostKey("SYSTEM", "SYS-ONLINE");
    }

    private boolean hasEvidence(String targetType, String targetId, String gateId) throws Exception {
        Map<String, Object> q = new HashMap<>();
        q.put("targetTypeCd", blank(targetType, "ASSET"));
        q.put("targetId", safe(targetId));
        q.put("offset", 0);
        q.put("pageSize", 5);
        List<Map<String, Object>> rows = auditDao.ifina1600S0_evidence(q);
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        if (isBlank(gateId)) {
            return true;
        }
        for (Map<String, Object> row : rows) {
            String g = as(row, "GATE_ID", "gateId");
            if (g == null || gateId.equalsIgnoreCase(g)) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal toBd(Object v) {
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static String as(Map<String, Object> row, String u, String c) {
        if (row == null) {
            return null;
        }
        Object v = row.get(u);
        if (v == null) {
            v = row.get(c);
        }
        if (v == null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && (e.getKey().equalsIgnoreCase(u) || e.getKey().equalsIgnoreCase(c))) {
                    v = e.getValue();
                    break;
                }
            }
        }
        return v == null ? null : String.valueOf(v);
    }

    private record CostKey(String targetTypeCd, String targetId) {}

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static String blank(String v, String d) {
        return isBlank(v) ? d : v.trim();
    }
}
