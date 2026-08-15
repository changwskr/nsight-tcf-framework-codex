package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.rule.GateEvaluationRule;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina9200DAO;

@Service
public class ifina9200Service {
    private final ifina9200DAO dao;
    private final GateEvaluationRule gateRule;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina9200Service(
            ifina9200DAO dao, GateEvaluationRule gateRule, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.dao = dao;
        this.gateRule = gateRule;
        this.changeLogWriter = changeLogWriter;
        this.authGuard = authGuard;
    }

    public ifina9200S0DTOout ifina9200S0(ifina9200S0DTOin input) throws Exception {
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = blank(input == null ? null : input.getTargetId(), "SG-WAS-A");
        String gateId = trim(input == null ? null : input.getGateId());

        List<Map<String, Object>> defs = dao.ifina9200S0_defs(Map.of());
        Map<String, Object> q = new HashMap<>();
        q.put("targetTypeCd", targetType);
        q.put("targetId", targetId);
        if (gateId != null) q.put("gateId", gateId);
        List<Map<String, Object>> rawResults = dao.ifina9200S0_results(q);
        Map<String, Map<String, Object>> byGate = new LinkedHashMap<>();
        if (rawResults != null) {
            for (Map<String, Object> row : rawResults) {
                byGate.put(as(row, "GATE_ID", "gateId"), mapResult(row));
            }
        }

        List<Map<String, Object>> gates = new ArrayList<>();
        int pass = 0, cond = 0, fail = 0, notReady = 0;
        if (defs != null) {
            for (Map<String, Object> d : defs) {
                String gid = as(d, "GATE_ID", "gateId");
                Map<String, Object> g = new HashMap<>();
                g.put("gateId", gid);
                g.put("nameKo", as(d, "NAME_KO", "nameKo"));
                g.put("description", as(d, "DESCRIPTION", "description"));
                g.put("sortNo", as(d, "SORT_NO", "sortNo"));
                Map<String, Object> res = byGate.get(gid);
                if (res == null) {
                    g.put("resultCd", "NOT_READY");
                    g.put("passYn", "N");
                    notReady++;
                } else {
                    g.putAll(res);
                    String rc = String.valueOf(res.get("resultCd"));
                    if ("PASS".equalsIgnoreCase(rc)) pass++;
                    else if ("CONDITIONAL".equalsIgnoreCase(rc)) cond++;
                    else if ("FAIL".equalsIgnoreCase(rc)) fail++;
                    else notReady++;
                }
                gates.add(g);
            }
        }

        ifina9200S0DTOout out = new ifina9200S0DTOout();
        out.setTargetTypeCd(targetType);
        out.setTargetId(targetId);
        out.setGates(gates);
        out.setResults(new ArrayList<>(byGate.values()));
        out.setPassCount(pass);
        out.setConditionalCount(cond);
        out.setFailCount(fail);
        out.setNotReadyCount(notReady);
        out.setHints(gateRule.softHints(gateId == null ? "" : gateId, targetType, targetId));
        return out;
    }

    public ifina9200U0DTOout ifina9200U0(ifina9200U0DTOin input) throws Exception {
        ifina9200U0DTOout out = new ifina9200U0DTOout();
        String gateId = trim(input == null ? null : input.getGateId());
        if (authGuard.denyIfHard(out, "ifina9200U0",
                gateId == null ? null : Map.of("gateId", gateId))) {
            return out;
        }
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = trim(input == null ? null : input.getTargetId());
        String resultCd = trim(input == null ? null : input.getResultCd());
        String evidence = empty(input == null ? null : input.getEvidence());
        if (gateId == null || targetId == null || resultCd == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: gateId, targetId, resultCd");
            return out;
        }
        ValidationResult vr = gateRule.evaluate(gateId, targetType, targetId, resultCd, evidence);
        if (vr.hasHard()) {
            RuleViolation h = vr.firstHard().orElseThrow();
            out.setPROC_CNT(0);
            out.setRSLT_CD(h.getRsltCd());
            out.setRSLT_MSG(h.formatted());
            out.setWarnings(vr.softWarnings());
            return out;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("gateId", gateId.toUpperCase(Locale.ROOT));
        p.put("targetTypeCd", targetType);
        p.put("targetId", targetId);
        p.put("resultCd", resultCd.toUpperCase(Locale.ROOT));
        p.put("passYn", "PASS".equalsIgnoreCase(resultCd) ? "Y" : "N");
        p.put("evidence", evidence);
        p.put("checkedBy", blank(input.getCheckedBy(), "LOCAL"));
        p.put("checkedAt", now());
        p.put("remark", empty(input.getRemark()));

        Map<String, Object> existing = dao.ifina9200S0_one(p);
        int cnt;
        if (existing == null || existing.isEmpty()) {
            p.put("resultId", "GR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
            cnt = dao.ifina9200U0_insert(p);
        } else {
            cnt = dao.ifina9200U0_update(p);
        }
        changeLogWriter.write("GATE", gateId + ":" + targetId, resultCd.toUpperCase(Locale.ROOT), existing, p, "ifina9200U0");
        out.setPROC_CNT(cnt);
        out.setRSLT_CD("0000");
        List<String> soft = new ArrayList<>(vr.softWarnings());
        soft.addAll(gateRule.softHints(gateId, targetType, targetId));
        if ("CONDITIONAL".equalsIgnoreCase(resultCd)) {
            soft.add("[RL-GT-005] CONDITIONAL → INF-020 워크리스트 후보");
        }
        out.setWarnings(soft);
        out.setRSLT_MSG(soft.isEmpty() ? "OK" : String.join("; ", soft));
        return out;
    }

    private static Map<String, Object> mapResult(Map<String, Object> row) {
        Map<String, Object> m = new HashMap<>();
        m.put("resultId", as(row, "RESULT_ID", "resultId"));
        m.put("gateId", as(row, "GATE_ID", "gateId"));
        m.put("gateName", as(row, "GATE_NAME", "gateName"));
        m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
        m.put("targetId", as(row, "TARGET_ID", "targetId"));
        m.put("resultCd", as(row, "RESULT_CD", "resultCd"));
        m.put("passYn", as(row, "PASS_YN", "passYn"));
        m.put("evidence", as(row, "EVIDENCE", "evidence"));
        m.put("checkedBy", as(row, "CHECKED_BY", "checkedBy"));
        m.put("checkedAt", as(row, "CHECKED_AT", "checkedAt"));
        m.put("remark", as(row, "REMARK", "remark"));
        return m;
    }

    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
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
