package nhnis.infra.in.a.application.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.V0ResponseMapper;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina6200DAO;

@Service
public class ifina6200Service {
    private final ifina6200DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina6200Service(ifina6200DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina6200S0DTOout ifina6200S0(ifina6200S0DTOin input) throws Exception {
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = blank(input == null ? null : input.getTargetId(), "SG-WAS-A");
        List<Map<String, Object>> raw = dao.ifina6200S0_S0(Map.of("targetTypeCd", targetType, "targetId", targetId));
        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                rows.add(mapRow(row));
            }
        }
        ifina6200S0DTOout out = new ifina6200S0DTOout();
        out.setTargetTypeCd(targetType);
        out.setTargetId(targetId);
        out.setRows(rows);
        List<String> warnings = evaluateSoft(rows);
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifinaV0DTOout ifina6200V0(ifina6200S0DTOin input) throws Exception {
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = blank(input == null ? null : input.getTargetId(), "SG-WAS-A");
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina6200S0_S0(Map.of("targetTypeCd", targetType, "targetId", targetId));
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                rows.add(mapRow(row));
            }
        }
        return V0ResponseMapper.from(evaluate(rows), targetType + ":" + targetId);
    }

    public ifina6200U0DTOout ifina6200U0(ifina6200U0DTOin input) throws Exception {
        ifina6200U0DTOout out = new ifina6200U0DTOout();
        if (authGuard.denyIfHard(out, "ifina6200U0")) return out;
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = trim(input == null ? null : input.getTargetId());
        String scope = blank(input == null ? null : input.getMetricScopeCd(), "CURRENT").toUpperCase(Locale.ROOT);
        if (targetId == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: targetId");
            return out;
        }
        if (!Set.of("CURRENT", "PEAK", "TARGET", "N1", "DR").contains(scope)) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("INVALID metricScopeCd: " + scope);
            return out;
        }
        Map<String, Object> key = Map.of("targetTypeCd", targetType, "targetId", targetId, "metricScopeCd", scope);
        Map<String, Object> before = dao.ifina6200S0_S1(key);
        Map<String, Object> p = new HashMap<>();
        p.put("targetTypeCd", targetType);
        p.put("targetId", targetId);
        p.put("metricScopeCd", scope);
        p.put("capturedAt", blank(input.getCapturedAt(), now()));
        p.put("cpuPct", input.getCpuPct());
        p.put("memPct", input.getMemPct());
        p.put("tps", input.getTps());
        p.put("respP95Ms", input.getRespP95Ms());
        p.put("dbConnPeak", input.getDbConnPeak());
        p.put("remark", empty(input.getRemark()));

        int cnt;
        if (dao.ifina6200S0_exists(p) > 0) {
            p.put("snapshotId", blank(input.getSnapshotId(), as(before, "SNAPSHOT_ID", "snapshotId")));
            cnt = dao.ifina6200U0_update(p);
        } else {
            p.put("snapshotId", blank(input.getSnapshotId(), "CAP-" + targetId + "-" + scope));
            p.put("regUserId", "LOCAL");
            p.put("regDtm", now());
            cnt = dao.ifina6200U0_insert(p);
        }
        changeLogWriter.write("CAPACITY", targetType + ":" + targetId + ":" + scope, "UPSERT", before, p, "ifina6200U0");

        List<Map<String, Object>> afterRows = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina6200S0_S0(Map.of("targetTypeCd", targetType, "targetId", targetId));
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                afterRows.add(mapRow(row));
            }
        }
        List<String> warnings = evaluateSoft(afterRows);
        out.setPROC_CNT(cnt);
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    /** RL-CP-001 Soft: N-1 TPS &lt; Peak TPS. RL-CP-002 Soft: TARGET 미입력. */
    public static List<String> evaluateSoft(List<Map<String, Object>> rows) {
        return evaluate(rows).softWarnings();
    }

    /** Gate3 PASS 시 Soft→HARD 승격에 재사용. */
    public static ValidationResult evaluate(List<Map<String, Object>> rows) {
        ValidationResult r = new ValidationResult();
        BigDecimal peakTps = null;
        BigDecimal n1Tps = null;
        boolean hasTarget = false;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String scope = str(row.get("metricScopeCd"));
                BigDecimal tps = toBd(row.get("tps"));
                if ("PEAK".equals(scope)) {
                    peakTps = tps;
                } else if ("N1".equals(scope)) {
                    n1Tps = tps;
                } else if ("TARGET".equals(scope)) {
                    hasTarget = true;
                }
            }
        }
        if (peakTps != null && n1Tps != null && n1Tps.compareTo(peakTps) < 0) {
            r.add(RuleViolation.soft("RL-CP-001", "N-1 TPS(" + n1Tps + ") < Peak TPS(" + peakTps + ")"));
        }
        if (!hasTarget) {
            r.add(RuleViolation.soft("RL-CP-002", "TARGET Snapshot 미입력"));
        }
        return r;
    }

    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("snapshotId", as(row, "SNAPSHOT_ID", "snapshotId"));
        m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
        m.put("targetId", as(row, "TARGET_ID", "targetId"));
        m.put("metricScopeCd", as(row, "METRIC_SCOPE_CD", "metricScopeCd"));
        m.put("capturedAt", as(row, "CAPTURED_AT", "capturedAt"));
        m.put("cpuPct", toBd(row.get("CPU_PCT") != null ? row.get("CPU_PCT") : row.get("cpuPct")));
        m.put("memPct", toBd(row.get("MEM_PCT") != null ? row.get("MEM_PCT") : row.get("memPct")));
        m.put("tps", toBd(row.get("TPS") != null ? row.get("TPS") : row.get("tps")));
        m.put("respP95Ms", toBd(row.get("RESP_P95_MS") != null ? row.get("RESP_P95_MS") : row.get("respP95Ms")));
        Object db = row.get("DB_CONN_PEAK");
        if (db == null) db = row.get("dbConnPeak");
        Integer dbPeak = null;
        if (db != null) {
            try { dbPeak = new BigDecimal(String.valueOf(db)).intValue(); } catch (Exception ignored) { }
        }
        m.put("dbConnPeak", dbPeak);
        m.put("remark", as(row, "REMARK", "remark"));
        return m;
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return null;
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception e) { return null; }
    }
    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
    private static String as(Map<String, Object> row, String u, String c) {
        if (row == null) return null;
        Object v = row.get(u);
        if (v == null) v = row.get(c);
        if (v == null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && (e.getKey().equalsIgnoreCase(u) || e.getKey().equalsIgnoreCase(c))) {
                    v = e.getValue(); break;
                }
            }
        }
        return v == null ? null : String.valueOf(v);
    }
}
