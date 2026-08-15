package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.V0ResponseMapper;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina6100DAO;

@Service
public class ifina6100Service {
    private final ifina6100DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina6100Service(ifina6100DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina6100S0DTOout ifina6100S0(ifina6100S0DTOin input) throws Exception {
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = blank(input == null ? null : input.getTargetId(), "SG-WAS-A");
        Map<String, Object> row = dao.ifina6100S0_S1(Map.of("targetTypeCd", targetType, "targetId", targetId));
        ifina6100S0DTOout out = new ifina6100S0DTOout();
        out.setTargetTypeCd(targetType);
        out.setTargetId(targetId);
        ValidationResult vr = evaluate(targetType, targetId, row);
        List<String> warnings = vr.softWarnings();
        if (row == null || row.isEmpty()) {
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            out.setWarnings(warnings);
            return out;
        }
        out.setProfileId(as(row, "PROFILE_ID", "profileId"));
        out.setOpsHoursCd(as(row, "OPS_HOURS_CD", "opsHoursCd"));
        out.setHaYn(as(row, "HA_YN", "haYn"));
        out.setHaModeCd(as(row, "HA_MODE_CD", "haModeCd"));
        out.setClusterYn(as(row, "CLUSTER_YN", "clusterYn"));
        out.setDrYn(as(row, "DR_YN", "drYn"));
        out.setDrModeCd(as(row, "DR_MODE_CD", "drModeCd"));
        out.setRtoMinutes(asInt(row, "RTO_MINUTES", "rtoMinutes"));
        out.setRpoMinutes(asInt(row, "RPO_MINUTES", "rpoMinutes"));
        out.setBackupYn(as(row, "BACKUP_YN", "backupYn"));
        out.setMonitoringYn(as(row, "MONITORING_YN", "monitoringYn"));
        out.setRemark(as(row, "REMARK", "remark"));
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifinaV0DTOout ifina6100V0(ifina6100S0DTOin input) throws Exception {
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = blank(input == null ? null : input.getTargetId(), "SG-WAS-A");
        Map<String, Object> row = dao.ifina6100S0_S1(Map.of("targetTypeCd", targetType, "targetId", targetId));
        return V0ResponseMapper.from(evaluate(targetType, targetId, row), targetType + ":" + targetId);
    }

    static ValidationResult evaluate(String targetType, String targetId, Map<String, Object> row) {
        ValidationResult r = new ValidationResult();
        if (row == null || row.isEmpty()) {
            r.add(RuleViolation.soft("RL-AV-001", "HA profile 없음"));
            return r;
        }
        if (!"Y".equalsIgnoreCase(as(row, "HA_YN", "haYn"))) {
            r.add(RuleViolation.soft("RL-AV-001", "HA_YN=N — Gate5 PASS 불가"));
        }
        if (as(row, "RTO_MINUTES", "rtoMinutes") == null || as(row, "RPO_MINUTES", "rpoMinutes") == null) {
            r.add(RuleViolation.soft("RL-AV-002", "RTO/RPO 미정 — Gate5 PASS 불가"));
        }
        return r;
    }

    public ifina6100U0DTOout ifina6100U0(ifina6100U0DTOin input) throws Exception {
        ifina6100U0DTOout out = new ifina6100U0DTOout();
        if (authGuard.denyIfHard(out, "ifina6100U0")) return out;
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = trim(input == null ? null : input.getTargetId());
        if (targetId == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: targetId");
            return out;
        }
        Map<String, Object> before = dao.ifina6100S0_S1(Map.of("targetTypeCd", targetType, "targetId", targetId));
        Map<String, Object> p = new HashMap<>();
        p.put("targetTypeCd", targetType);
        p.put("targetId", targetId);
        p.put("opsHoursCd", blank(input.getOpsHoursCd(), "24X365"));
        p.put("haYn", blank(input.getHaYn(), "N").toUpperCase(Locale.ROOT));
        p.put("haModeCd", empty(input.getHaModeCd()));
        p.put("clusterYn", blank(input.getClusterYn(), "N").toUpperCase(Locale.ROOT));
        p.put("drYn", blank(input.getDrYn(), "N").toUpperCase(Locale.ROOT));
        p.put("drModeCd", empty(input.getDrModeCd()));
        p.put("rtoMinutes", input.getRtoMinutes());
        p.put("rpoMinutes", input.getRpoMinutes());
        p.put("backupYn", blank(input.getBackupYn(), "N").toUpperCase(Locale.ROOT));
        p.put("monitoringYn", blank(input.getMonitoringYn(), "N").toUpperCase(Locale.ROOT));
        p.put("remark", empty(input.getRemark()));

        List<String> warnings = new ArrayList<>();
        if (!"Y".equals(p.get("haYn"))) {
            warnings.add("[RL-AV-001] HA_YN=N (저장 Soft 허용)");
        }
        if (p.get("rtoMinutes") == null || p.get("rpoMinutes") == null) {
            warnings.add("[RL-AV-002] RTO/RPO 미정 (저장 Soft 허용)");
        }

        int cnt;
        if (dao.ifina6100S0_exists(p) > 0) {
            p.put("chgUserId", "LOCAL");
            p.put("chgDtm", now());
            cnt = dao.ifina6100U0_update(p);
        } else {
            p.put("profileId", blank(input.getProfileId(), "AV-" + targetId));
            p.put("regUserId", "LOCAL");
            p.put("regDtm", now());
            cnt = dao.ifina6100U0_insert(p);
        }
        changeLogWriter.write("AVAIL", targetType + ":" + targetId, "UPSERT", before, p, "ifina6100U0");
        out.setPROC_CNT(cnt);
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static Integer asInt(Map<String, Object> row, String u, String c) {
        String s = as(row, u, c);
        if (s == null) return null;
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }
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
