package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.ifina2200C0DTOin;
import nhnis.infra.in.a.dto.ifina2200C0DTOout;
import nhnis.infra.in.a.dto.ifina2200D0DTOin;
import nhnis.infra.in.a.dto.ifina2200D0DTOout;
import nhnis.infra.in.a.dto.ifina2200S0DTOSub0;
import nhnis.infra.in.a.dto.ifina2200S0DTOin;
import nhnis.infra.in.a.dto.ifina2200S0DTOout;
import nhnis.infra.in.a.dto.ifina2200U0DTOin;
import nhnis.infra.in.a.dto.ifina2200U0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina2100DAO;
import nhnis.infra.in.a.persistence.dao.ifina2200DAO;

@Service
public class ifina2200Service {

    private final ifina2200DAO ifina2200DAO;
    private final ifina2100DAO ifina2100DAO;
    private final AuthGuard authGuard;

    public ifina2200Service(ifina2200DAO ifina2200DAO, ifina2100DAO ifina2100DAO, AuthGuard authGuard) {
        this.ifina2200DAO = ifina2200DAO;
        this.ifina2100DAO = ifina2100DAO;
        this.authGuard = authGuard;
    }

    public ifina2200S0DTOout ifina2200S0(ifina2200S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "appId", input.getAppId());
            put(param, "appName", input.getAppName());
            put(param, "systemId", input.getSystemId());
            put(param, "statusCd", input.getStatusCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        int total = ifina2200DAO.ifina2200S0_S0_count(param);
        List<Map<String, Object>> rows = ifina2200DAO.ifina2200S0_S0(param);
        ifina2200S0DTOout out = new ifina2200S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                ifina2200S0DTOSub0 sub = new ifina2200S0DTOSub0();
                sub.setAppId(as(row, "APP_ID", "appId"));
                sub.setAppName(as(row, "APP_NAME", "appName"));
                sub.setSystemId(as(row, "SYSTEM_ID", "systemId"));
                sub.setLangCd(as(row, "LANG_CD", "langCd"));
                sub.setStatusCd(as(row, "STATUS_CD", "statusCd"));
                sub.setRemark(as(row, "REMARK", "remark"));
                sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                out.addifina2200S0DTOSub0(sub);
            }
        }
        out.setSize(out.sizeifina2200S0DTOSub0());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina2200C0DTOout ifina2200C0(ifina2200C0DTOin input) throws Exception {
        ifina2200C0DTOout out = new ifina2200C0DTOout();
        if (authGuard.denyIfHard(out, "ifina2200C0")) return out;
        String appId = trim(input == null ? null : input.getAppId());
        String appName = trim(input == null ? null : input.getAppName());
        String systemId = trim(input == null ? null : input.getSystemId());
        if (appId == null || appName == null || systemId == null) {
            out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG("REQUIRED: appId, appName, systemId");
            return out;
        }
        if (ifina2100DAO.ifina2100S0_S0_exists(Map.of("systemId", systemId)) <= 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0004"); out.setRSLT_MSG("[RL-FK-001] 시스템 없음: " + systemId);
            return out;
        }
        if (ifina2200DAO.ifina2200S0_S0_exists(Map.of("appId", appId)) > 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0002"); out.setRSLT_MSG("DUPLICATE_APP_ID");
            return out;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("appId", appId);
        param.put("appName", appName);
        param.put("systemId", systemId);
        param.put("langCd", blank(input.getLangCd(), "JAVA"));
        param.put("statusCd", blank(input.getStatusCd(), "DISCOVERED"));
        param.put("remark", empty(input.getRemark()));
        param.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
        param.put("regDtm", now());
        out.setPROC_CNT(ifina2200DAO.ifina2200C0_C0(param));
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    public ifina2200U0DTOout ifina2200U0(ifina2200U0DTOin input) throws Exception {
        ifina2200U0DTOout out = new ifina2200U0DTOout();
        if (authGuard.denyIfHard(out, "ifina2200U0")) return out;
        String appId = trim(input == null ? null : input.getAppId());
        String appName = trim(input == null ? null : input.getAppName());
        String systemId = trim(input == null ? null : input.getSystemId());
        if (appId == null || appName == null || systemId == null) {
            out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG("REQUIRED: appId, appName, systemId");
            return out;
        }
        if (ifina2200DAO.ifina2200S0_S0_exists(Map.of("appId", appId)) <= 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0003"); out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        if (ifina2100DAO.ifina2100S0_S0_exists(Map.of("systemId", systemId)) <= 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0004"); out.setRSLT_MSG("[RL-FK-001] 시스템 없음: " + systemId);
            return out;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("appId", appId);
        param.put("appName", appName);
        param.put("systemId", systemId);
        param.put("langCd", empty(input.getLangCd()));
        param.put("statusCd", empty(input.getStatusCd()));
        param.put("remark", empty(input.getRemark()));
        param.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
        param.put("chgDtm", now());
        out.setPROC_CNT(ifina2200DAO.ifina2200U0_U0(param));
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    public ifina2200D0DTOout ifina2200D0(ifina2200D0DTOin input) throws Exception {
        ifina2200D0DTOout out = new ifina2200D0DTOout();
        if (authGuard.denyIfHard(out, "ifina2200D0")) return out;
        if (input == null || input.getAppIdList() == null || input.getAppIdList().isEmpty()) {
            out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getAppIdList().stream()
                .filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
        if (ids.isEmpty()) {
            out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG("NO_DATA");
            return out;
        }
        out.setPROC_CNT(ifina2200DAO.ifina2200D0_D0(Map.of("appIdList", ids)));
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    private static int pageNo(Integer v) { return v == null || v <= 0 ? 1 : v; }
    private static int pageSize(Integer v) { int s = v == null || v <= 0 ? 10 : v; return Math.min(s, 100); }
    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static void put(Map<String, Object> m, String k, String v) { if (v != null && !v.isBlank()) m.put(k, v.trim()); }
    private static String as(Map<String, Object> row, String u, String c) {
        if (row == null) return null;
        Object v = row.get(u); if (v == null) v = row.get(c);
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
