package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.rule.WaveConflictRule;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.V0ResponseMapper;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina8200DAO;

@Service
public class ifina8200Service {
    private final ifina8200DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final WaveConflictRule waveConflictRule;
    private final AuthGuard authGuard;

    public ifina8200Service(
            ifina8200DAO dao, ChangeLogWriter changeLogWriter, WaveConflictRule waveConflictRule, AuthGuard authGuard) {
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
        this.waveConflictRule = waveConflictRule;
        this.authGuard = authGuard;
    }

    public ifina8200S0DTOout ifina8200S0(ifina8200S0DTOin input) throws Exception {
        List<Map<String, Object>> raw = dao.ifina8200S0_S0(Map.of());
        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                if (input != null && trim(input.getWaveId()) != null
                        && !trim(input.getWaveId()).equalsIgnoreCase(as(row, "WAVE_ID", "waveId"))) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("waveId", as(row, "WAVE_ID", "waveId"));
                m.put("waveName", as(row, "WAVE_NAME", "waveName"));
                m.put("sequenceNo", asInt(row, "SEQUENCE_NO", "sequenceNo"));
                m.put("plannedStart", as(row, "PLANNED_START", "plannedStart"));
                m.put("plannedEnd", as(row, "PLANNED_END", "plannedEnd"));
                m.put("statusCd", as(row, "STATUS_CD", "statusCd"));
                m.put("remark", as(row, "REMARK", "remark"));
                m.put("planCnt", asInt(row, "PLAN_CNT", "planCnt"));
                rows.add(m);
            }
        }
        List<String> warnings = waveConflictRule.evaluate().softWarnings();
        ifina8200S0DTOout out = new ifina8200S0DTOout();
        out.setRows(rows);
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifinaV0DTOout ifina8200V0(ifina8200S0DTOin input) throws Exception {
        return V0ResponseMapper.from(waveConflictRule.evaluate(), "WAVE");
    }

    public ifina8200U0DTOout ifina8200U0(ifina8200U0DTOin input) throws Exception {
        ifina8200U0DTOout out = new ifina8200U0DTOout();
        if (authGuard.denyIfHard(out, "ifina8200U0")) return out;
        String waveId = trim(input == null ? null : input.getWaveId());
        String waveName = trim(input == null ? null : input.getWaveName());
        if (waveId == null || waveName == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: waveId, waveName");
            return out;
        }
        Map<String, Object> before = dao.ifina8200S0_S1(Map.of("waveId", waveId));
        Map<String, Object> p = new HashMap<>();
        p.put("waveId", waveId);
        p.put("waveName", waveName);
        p.put("sequenceNo", input.getSequenceNo() == null ? 99 : input.getSequenceNo());
        p.put("plannedStart", empty(input.getPlannedStart()));
        p.put("plannedEnd", empty(input.getPlannedEnd()));
        p.put("statusCd", blank(input.getStatusCd(), "PLANNED").toUpperCase(Locale.ROOT));
        p.put("remark", empty(input.getRemark()));
        int cnt;
        if (dao.ifina8200S0_exists(p) > 0) {
            p.put("chgUserId", "LOCAL");
            p.put("chgDtm", now());
            cnt = dao.ifina8200U0_update(p);
        } else {
            p.put("regUserId", "LOCAL");
            p.put("regDtm", now());
            cnt = dao.ifina8200U0_insert(p);
        }
        changeLogWriter.write("MIG_WAVE", waveId, "UPSERT", before, p, "ifina8200U0");
        out.setPROC_CNT(cnt);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static Integer asInt(Map<String, Object> row, String u, String c) {
        String s = as(row, u, c);
        if (s == null) return null;
        try { return new java.math.BigDecimal(s).intValue(); } catch (Exception e) { return null; }
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
