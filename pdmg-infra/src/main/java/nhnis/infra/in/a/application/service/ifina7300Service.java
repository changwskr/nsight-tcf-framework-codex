package nhnis.infra.in.a.application.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina7300DAO;

@Service
public class ifina7300Service {
    private static final Set<String> SCENARIOS = Set.of("ASIS", "TOBE", "MIG");
    private static final Set<String> COST_TYPES = Set.of("HW", "CLOUD", "MW", "DB", "OPS", "OTHER", "MIGRATION");

    private final ifina7300DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina7300Service(ifina7300DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
        this.authGuard = authGuard;
    }

    public ifina7300S0DTOout ifina7300S0(ifina7300S0DTOin input) throws Exception {
        ifina7300S0DTOout out = new ifina7300S0DTOout();
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "SYSTEM");
        String targetId = blank(input == null ? null : input.getTargetId(), "SYS-ONLINE");
        String periodYm = blank(input == null ? null : input.getPeriodYm(), "202608");
        int years = input != null && input.getYears() != null && input.getYears() > 0
                ? Math.min(input.getYears(), 10) : 5;

        Map<String, Object> param = new HashMap<>();
        param.put("targetTypeCd", targetType);
        param.put("targetId", targetId);
        param.put("periodYm", periodYm);
        if (input != null && trim(input.getScenarioCd()) != null) {
            param.put("scenarioCd", input.getScenarioCd().trim().toUpperCase(Locale.ROOT));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal asisAnnual = BigDecimal.ZERO;
        BigDecimal tobeAnnual = BigDecimal.ZERO;
        BigDecimal migrationOnce = BigDecimal.ZERO;

        List<Map<String, Object>> raw = dao.ifina7300S0_S0(param);
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Map<String, Object> m = mapRow(row);
                rows.add(m);
                String scenario = str(m.get("scenarioCd"));
                BigDecimal amount = toBd(m.get("amount"));
                if (amount == null) amount = BigDecimal.ZERO;
                if ("ASIS".equals(scenario)) asisAnnual = asisAnnual.add(amount);
                else if ("TOBE".equals(scenario)) tobeAnnual = tobeAnnual.add(amount);
                else if ("MIG".equals(scenario)) migrationOnce = migrationOnce.add(amount);
            }
        }

        BigDecimal yearsBd = BigDecimal.valueOf(years);
        BigDecimal asisTco = asisAnnual.multiply(yearsBd);
        BigDecimal tobeTco = tobeAnnual.multiply(yearsBd).add(migrationOnce);
        BigDecimal delta = asisTco.subtract(tobeTco);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("years", years);
        summary.put("asisAnnual", asisAnnual);
        summary.put("tobeAnnual", tobeAnnual);
        summary.put("migrationOnce", migrationOnce);
        summary.put("asisTco", asisTco);
        summary.put("tobeTco", tobeTco);
        summary.put("deltaTco", delta);
        summary.put("currencyCd", "KRW");

        out.setTargetTypeCd(targetType);
        out.setTargetId(targetId);
        out.setPeriodYm(periodYm);
        out.setYears(years);
        out.setRows(rows);
        out.setTcoSummary(summary);
        out.setAsisAnnual(asisAnnual);
        out.setTobeAnnual(tobeAnnual);
        out.setMigrationOnce(migrationOnce);
        out.setAsisTco(asisTco);
        out.setTobeTco(tobeTco);
        out.setDeltaTco(delta);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina7300C0DTOout ifina7300C0(ifina7300C0DTOin input) throws Exception {
        ifina7300C0DTOout out = new ifina7300C0DTOout();
        if (authGuard.denyIfHard(out, "ifina7300C0")) {
            return out;
        }
        String err = validate(input);
        if (err != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(err);
            return out;
        }
        String costId = trim(input.getCostId());
        if (dao.ifina7300S0_exists(Map.of("costId", costId)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_COST_ID");
            return out;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("costId", costId);
        p.put("targetTypeCd", blank(input.getTargetTypeCd(), "SYSTEM").toUpperCase(Locale.ROOT));
        p.put("targetId", trim(input.getTargetId()));
        p.put("periodYm", trim(input.getPeriodYm()));
        p.put("scenarioCd", trim(input.getScenarioCd()).toUpperCase(Locale.ROOT));
        p.put("costTypeCd", trim(input.getCostTypeCd()).toUpperCase(Locale.ROOT));
        p.put("amount", input.getAmount());
        p.put("currencyCd", blank(input.getCurrencyCd(), "KRW"));
        p.put("remark", empty(input.getRemark()));
        p.put("regUserId", "LOCAL");
        p.put("regDtm", now());
        out.setPROC_CNT(dao.ifina7300C0_C0(p));
        changeLogWriter.write("COST", costId, "CREATE", null, p, "ifina7300C0");
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static String validate(ifina7300C0DTOin input) {
        if (input == null || trim(input.getCostId()) == null || trim(input.getTargetId()) == null
                || trim(input.getPeriodYm()) == null || trim(input.getScenarioCd()) == null
                || trim(input.getCostTypeCd()) == null || input.getAmount() == null) {
            return "REQUIRED: costId, targetId, periodYm, scenarioCd, costTypeCd, amount";
        }
        String sc = input.getScenarioCd().trim().toUpperCase(Locale.ROOT);
        if (!SCENARIOS.contains(sc)) return "INVALID: scenarioCd (ASIS|TOBE|MIG)";
        String ct = input.getCostTypeCd().trim().toUpperCase(Locale.ROOT);
        if (!COST_TYPES.contains(ct)) return "INVALID: costTypeCd";
        if (input.getAmount().compareTo(BigDecimal.ZERO) < 0) return "INVALID: amount";
        String ym = input.getPeriodYm().trim();
        if (!ym.matches("\\d{6}")) return "INVALID: periodYm (yyyyMM)";
        return null;
    }

    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("costId", as(row, "COST_ID", "costId"));
        m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
        m.put("targetId", as(row, "TARGET_ID", "targetId"));
        m.put("periodYm", as(row, "PERIOD_YM", "periodYm"));
        m.put("scenarioCd", as(row, "SCENARIO_CD", "scenarioCd"));
        m.put("costTypeCd", as(row, "COST_TYPE_CD", "costTypeCd"));
        m.put("amount", toBd(val(row, "AMOUNT", "amount")));
        m.put("currencyCd", as(row, "CURRENCY_CD", "currencyCd"));
        m.put("remark", as(row, "REMARK", "remark"));
        return m;
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return null;
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception e) { return null; }
    }
    private static Object val(Map<String, Object> row, String u, String c) {
        Object v = row.get(u);
        if (v == null) v = row.get(c);
        return v;
    }
    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
    private static String as(Map<String, Object> row, String u, String c) {
        Object v = val(row, u, c);
        if (v == null && row != null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && (e.getKey().equalsIgnoreCase(u) || e.getKey().equalsIgnoreCase(c))) {
                    v = e.getValue(); break;
                }
            }
        }
        return v == null ? null : String.valueOf(v);
    }
}
