package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina5200DAO;

@Service
public class ifina5200Service {
    private static final Set<String> DIRECTIONS = Set.of("INBOUND", "OUTBOUND", "BIDIRECTIONAL");

    private final ifina5200DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina5200Service(ifina5200DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina5200S0DTOout ifina5200S0(ifina5200S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "interfaceId", input.getInterfaceId());
            put(param, "fromAppId", input.getFromAppId());
            put(param, "toAppId", input.getToAppId());
            put(param, "protocolCd", input.getProtocolCd());
            put(param, "directionCd", input.getDirectionCd());
            put(param, "criticalYn", input.getCriticalYn());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina5200S0_S0(param);
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                rows.add(mapRow(row));
            }
        }
        int total = dao.ifina5200S0_S0_count(param);
        ifina5200S0DTOout out = new ifina5200S0DTOout();
        out.setRows(rows);
        out.setSize(rows.size());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina5200C0DTOout ifina5200C0(ifina5200C0DTOin input) throws Exception {
        ifina5200C0DTOout out = new ifina5200C0DTOout();
        if (authGuard.denyIfHard(out, "ifina5200C0")) return out;
        String err = validate(input);
        if (err != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(err);
            return out;
        }
        String id = trim(input.getInterfaceId());
        if (dao.ifina5200S0_exists(Map.of("interfaceId", id)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_INTERFACE_ID");
            return out;
        }
        String fk = validateApps(input.getFromAppId(), input.getToAppId());
        if (fk != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(fk);
            return out;
        }
        Map<String, Object> p = toParam(input);
        p.put("regUserId", "LOCAL");
        p.put("regDtm", now());
        out.setPROC_CNT(dao.ifina5200C0_C0(p));
        changeLogWriter.write("INTERFACE", id, "CREATE", null, p, "ifina5200C0");
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina5200U0DTOout ifina5200U0(ifina5200U0DTOin input) throws Exception {
        ifina5200U0DTOout out = new ifina5200U0DTOout();
        if (authGuard.denyIfHard(out, "ifina5200U0")) return out;
        String err = validate(input);
        if (err != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(err);
            return out;
        }
        String id = trim(input.getInterfaceId());
        Map<String, Object> before = dao.ifina5200S0_S1(Map.of("interfaceId", id));
        if (before == null || before.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        String fk = validateApps(input.getFromAppId(), input.getToAppId());
        if (fk != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(fk);
            return out;
        }
        Map<String, Object> p = toParam(input);
        p.put("chgUserId", "LOCAL");
        p.put("chgDtm", now());
        out.setPROC_CNT(dao.ifina5200U0_U0(p));
        changeLogWriter.write("INTERFACE", id, "UPDATE", before, p, "ifina5200U0");
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina5200D0DTOout ifina5200D0(ifina5200D0DTOin input) throws Exception {
        ifina5200D0DTOout out = new ifina5200D0DTOout();
        if (authGuard.denyIfHard(out, "ifina5200D0")) return out;
        if (input == null || input.getInterfaceIdList() == null || input.getInterfaceIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getInterfaceIdList().stream()
                .filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
        if (ids.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        out.setPROC_CNT(dao.ifina5200D0_D0(Map.of("interfaceIdList", ids)));
        for (String id : ids) {
            changeLogWriter.write("INTERFACE", id, "DELETE", Map.of("interfaceId", id), null, "ifina5200D0");
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private String validateApps(String fromAppId, String toAppId) throws Exception {
        if (dao.ifina5200S0_appExists(Map.of("appId", trim(fromAppId))) <= 0) {
            return "[HARD] fromAppId 미존재: " + fromAppId;
        }
        String to = trim(toAppId);
        if (to != null && dao.ifina5200S0_appExists(Map.of("appId", to)) <= 0) {
            return "[HARD] toAppId 미존재: " + to;
        }
        return null;
    }

    private static String validate(ifina5200C0DTOin input) {
        if (input == null || trim(input.getInterfaceId()) == null || trim(input.getFromAppId()) == null) {
            return "REQUIRED: interfaceId, fromAppId";
        }
        String toApp = trim(input.getToAppId());
        String ext = trim(input.getToExternalName());
        if (toApp == null && ext == null) {
            return "REQUIRED: toAppId 또는 toExternalName";
        }
        if (toApp != null && toApp.equals(trim(input.getFromAppId()))) {
            return "INVALID: fromAppId와 toAppId가 동일";
        }
        String dir = blank(input.getDirectionCd(), "OUTBOUND").toUpperCase(Locale.ROOT);
        if (!DIRECTIONS.contains(dir)) {
            return "INVALID: directionCd (INBOUND|OUTBOUND|BIDIRECTIONAL)";
        }
        return null;
    }

    private static Map<String, Object> toParam(ifina5200C0DTOin input) {
        Map<String, Object> p = new HashMap<>();
        p.put("interfaceId", trim(input.getInterfaceId()));
        p.put("fromAppId", trim(input.getFromAppId()));
        p.put("toAppId", empty(input.getToAppId()));
        p.put("toExternalName", empty(input.getToExternalName()));
        p.put("protocolCd", blank(input.getProtocolCd(), "HTTP").toUpperCase(Locale.ROOT));
        p.put("directionCd", blank(input.getDirectionCd(), "OUTBOUND").toUpperCase(Locale.ROOT));
        p.put("criticalYn", blank(input.getCriticalYn(), "N").toUpperCase(Locale.ROOT));
        p.put("remark", empty(input.getRemark()));
        return p;
    }

    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("interfaceId", as(row, "INTERFACE_ID", "interfaceId"));
        m.put("fromAppId", as(row, "FROM_APP_ID", "fromAppId"));
        m.put("toAppId", as(row, "TO_APP_ID", "toAppId"));
        m.put("toExternalName", as(row, "TO_EXTERNAL_NAME", "toExternalName"));
        m.put("protocolCd", as(row, "PROTOCOL_CD", "protocolCd"));
        m.put("directionCd", as(row, "DIRECTION_CD", "directionCd"));
        m.put("criticalYn", as(row, "CRITICAL_YN", "criticalYn"));
        m.put("remark", as(row, "REMARK", "remark"));
        return m;
    }

    private static int pageNo(Integer v) { return v == null || v <= 0 ? 1 : v; }
    private static int pageSize(Integer v) { int s = v == null || v <= 0 ? 50 : v; return Math.min(s, 200); }
    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static void put(Map<String, Object> m, String k, String v) { if (v != null && !v.isBlank()) m.put(k, v.trim()); }
    private static Object val(Map<String, Object> row, String u, String c) {
        Object v = row.get(u);
        if (v == null) v = row.get(c);
        return v;
    }
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
