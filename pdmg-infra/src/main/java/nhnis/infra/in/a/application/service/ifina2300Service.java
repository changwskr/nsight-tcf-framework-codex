package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina2300DAO;

@Service
public class ifina2300Service {
    private final ifina2300DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina2300Service(ifina2300DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina2300S0DTOout ifina2300S0(ifina2300S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "appId", input.getAppId());
            put(param, "mapTypeCd", input.getMapTypeCd());
            put(param, "refId", input.getRefId());
            put(param, "roleCd", input.getRoleCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina2300S0_S0(param);
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                rows.add(mapRow(row));
            }
        }
        int total = dao.ifina2300S0_S0_count(param);
        ifina2300S0DTOout out = new ifina2300S0DTOout();
        out.setRows(rows);
        out.setSize(rows.size());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina2300C0DTOout ifina2300C0(ifina2300C0DTOin input) throws Exception {
        ifina2300C0DTOout out = new ifina2300C0DTOout();
        if (authGuard.denyIfHard(out, "ifina2300C0")) return out;
        String mapId = trim(input == null ? null : input.getMapId());
        String appId = trim(input == null ? null : input.getAppId());
        String refId = trim(input == null ? null : input.getRefId());
        String mapType = blank(input == null ? null : input.getMapTypeCd(), "GROUP").toUpperCase(Locale.ROOT);
        if (mapId == null || appId == null || refId == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: mapId, appId, refId");
            return out;
        }
        if (!Set.of("SERVER", "GROUP", "DB").contains(mapType)) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("INVALID mapTypeCd: " + mapType);
            return out;
        }
        if (dao.ifina2300S0_S0_exists(Map.of("mapId", mapId)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_MAP_ID");
            return out;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("mapId", mapId);
        p.put("appId", appId);
        p.put("mapTypeCd", mapType);
        p.put("refId", refId);
        p.put("roleCd", empty(input.getRoleCd()));
        p.put("remark", empty(input.getRemark()));
        p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
        p.put("regDtm", now());
        out.setPROC_CNT(dao.ifina2300C0_C0(p));
        changeLogWriter.write("APP_MAP", mapId, "CREATE", null, p, "ifina2300C0");
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina2300D0DTOout ifina2300D0(ifina2300D0DTOin input) throws Exception {
        ifina2300D0DTOout out = new ifina2300D0DTOout();
        if (authGuard.denyIfHard(out, "ifina2300D0")) return out;
        if (input == null || input.getMapIdList() == null || input.getMapIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getMapIdList().stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        out.setPROC_CNT(dao.ifina2300D0_D0(Map.of("mapIdList", ids)));
        for (String id : ids) {
            changeLogWriter.write("APP_MAP", id, "DELETE", Map.of("mapId", id), null, "ifina2300D0");
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mapId", as(row, "MAP_ID", "mapId"));
        m.put("appId", as(row, "APP_ID", "appId"));
        m.put("mapTypeCd", as(row, "MAP_TYPE_CD", "mapTypeCd"));
        m.put("refId", as(row, "REF_ID", "refId"));
        m.put("roleCd", as(row, "ROLE_CD", "roleCd"));
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
