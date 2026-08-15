package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina8100DAO;

@Service
public class ifina8100Service {
    private static final Set<String> VALID_7R = Set.of(
            "REHOST", "REPLATFORM", "REFACTOR", "REPURCHASE", "RETAIN", "RETIRE", "RELOCATE");

    private final ifina8100DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina8100Service(ifina8100DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
        this.authGuard = authGuard;
    }

    public ifina8100S0DTOout ifina8100S0(ifina8100S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "waveId", input.getWaveId());
            put(param, "waveUnassigned", input.getWaveUnassigned());
            put(param, "targetTypeCd", input.getTargetTypeCd());
            put(param, "targetId", input.getTargetId());
            put(param, "strategy7rCd", input.getStrategy7rCd());
            put(param, "statusCd", input.getStatusCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina8100S0_S0(param);
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Map<String, Object> m = mapRow(row);
                rows.add(m);
                if (m.get("waveId") == null || String.valueOf(m.get("waveId")).isBlank()) {
                    warnings.add("[RL-MG-003] Wave 미배정: " + m.get("planId"));
                }
            }
        }
        int total = dao.ifina8100S0_S0_count(param);
        ifina8100S0DTOout out = new ifina8100S0DTOout();
        out.setRows(rows);
        out.setWarnings(warnings.stream().distinct().toList());
        out.setSize(rows.size());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina8100C0DTOout ifina8100C0(ifina8100C0DTOin input) throws Exception {
        ifina8100C0DTOout out = new ifina8100C0DTOout();
        if (authGuard.denyIfHard(out, "ifina8100C0")) return out;
        String err = validate(input);
        if (err != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(err);
            return out;
        }
        String planId = trim(input.getPlanId());
        if (dao.ifina8100S0_exists(Map.of("planId", planId)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_PLAN_ID");
            return out;
        }
        Map<String, Object> p = toParam(input);
        p.put("regUserId", "LOCAL");
        p.put("regDtm", now());
        out.setPROC_CNT(dao.ifina8100C0_C0(p));
        List<String> warnings = soft(p);
        changeLogWriter.write("MIG_PLAN", planId, "CREATE", null, p, "ifina8100C0");
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifina8100U0DTOout ifina8100U0(ifina8100U0DTOin input) throws Exception {
        ifina8100U0DTOout out = new ifina8100U0DTOout();
        if (authGuard.denyIfHard(out, "ifina8100U0")) return out;
        String err = validate(input);
        if (err != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(err);
            return out;
        }
        String planId = trim(input.getPlanId());
        Map<String, Object> before = dao.ifina8100S0_S1(Map.of("planId", planId));
        if (before == null || before.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        Map<String, Object> p = toParam(input);
        p.put("chgUserId", "LOCAL");
        p.put("chgDtm", now());
        out.setPROC_CNT(dao.ifina8100U0_U0(p));
        List<String> warnings = soft(p);
        changeLogWriter.write("MIG_PLAN", planId, "UPDATE", before, p, "ifina8100U0");
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifina8100D0DTOout ifina8100D0(ifina8100D0DTOin input) throws Exception {
        ifina8100D0DTOout out = new ifina8100D0DTOout();
        if (authGuard.denyIfHard(out, "ifina8100D0")) return out;
        if (input == null || input.getPlanIdList() == null || input.getPlanIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getPlanIdList().stream()
                .filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
        if (ids.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        out.setPROC_CNT(dao.ifina8100D0_D0(Map.of("planIdList", ids)));
        for (String id : ids) {
            changeLogWriter.write("MIG_PLAN", id, "DELETE", Map.of("planId", id), null, "ifina8100D0");
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static String validate(ifina8100C0DTOin input) {
        if (input == null) return "REQUIRED: planId, targetId, strategy7rCd, targetPlatformCd";
        if (trim(input.getPlanId()) == null || trim(input.getTargetId()) == null) {
            return "REQUIRED: planId, targetId";
        }
        String s7 = blank(input.getStrategy7rCd(), "").toUpperCase(Locale.ROOT);
        if (!VALID_7R.contains(s7)) {
            return "[RL-MG-002] INVALID strategy7rCd: " + input.getStrategy7rCd();
        }
        if (trim(input.getTargetPlatformCd()) == null) {
            return "[RL-MG-001] targetPlatformCd 필수";
        }
        return null;
    }

    private static List<String> soft(Map<String, Object> p) {
        List<String> w = new ArrayList<>();
        Object wave = p.get("waveId");
        if (wave == null || String.valueOf(wave).isBlank()) {
            w.add("[RL-MG-003] Wave 미배정");
        }
        return w;
    }

    private static Map<String, Object> toParam(ifina8100C0DTOin input) {
        Map<String, Object> p = new HashMap<>();
        p.put("planId", trim(input.getPlanId()));
        p.put("waveId", emptyToNull(input.getWaveId()));
        p.put("targetTypeCd", blank(input.getTargetTypeCd(), "GROUP").toUpperCase(Locale.ROOT));
        p.put("targetId", trim(input.getTargetId()));
        p.put("strategy7rCd", blank(input.getStrategy7rCd(), "").toUpperCase(Locale.ROOT));
        p.put("currentPlatformCd", empty(input.getCurrentPlatformCd()));
        p.put("targetPlatformCd", blank(input.getTargetPlatformCd(), "").toUpperCase(Locale.ROOT));
        p.put("difficultyCd", blank(input.getDifficultyCd(), "M").toUpperCase(Locale.ROOT));
        p.put("statusCd", blank(input.getStatusCd(), "TARGET_DEFINED").toUpperCase(Locale.ROOT));
        p.put("remark", empty(input.getRemark()));
        return p;
    }

    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("planId", as(row, "PLAN_ID", "planId"));
        m.put("waveId", as(row, "WAVE_ID", "waveId"));
        m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
        m.put("targetId", as(row, "TARGET_ID", "targetId"));
        m.put("strategy7rCd", as(row, "STRATEGY_7R_CD", "strategy7rCd"));
        m.put("currentPlatformCd", as(row, "CURRENT_PLATFORM_CD", "currentPlatformCd"));
        m.put("targetPlatformCd", as(row, "TARGET_PLATFORM_CD", "targetPlatformCd"));
        m.put("difficultyCd", as(row, "DIFFICULTY_CD", "difficultyCd"));
        m.put("statusCd", as(row, "STATUS_CD", "statusCd"));
        m.put("remark", as(row, "REMARK", "remark"));
        return m;
    }

    private static int pageNo(Integer v) { return v == null || v <= 0 ? 1 : v; }
    private static int pageSize(Integer v) { int s = v == null || v <= 0 ? 50 : v; return Math.min(s, 200); }
    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String emptyToNull(String v) { return trim(v); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static void put(Map<String, Object> m, String k, String v) { if (v != null && !v.isBlank()) m.put(k, v.trim()); }
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
