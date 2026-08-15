package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;

import nhnis.infra.in.a.dto.ifina4200C0DTOin;
import nhnis.infra.in.a.dto.ifina4200C0DTOout;
import nhnis.infra.in.a.dto.ifina4200D0DTOin;
import nhnis.infra.in.a.dto.ifina4200D0DTOout;
import nhnis.infra.in.a.dto.ifina4200S0DTOSub0;
import nhnis.infra.in.a.dto.ifina4200S0DTOin;
import nhnis.infra.in.a.dto.ifina4200S0DTOout;
import nhnis.infra.in.a.dto.ifina4200U0DTOin;
import nhnis.infra.in.a.dto.ifina4200U0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina1999DAO;
import nhnis.infra.in.a.persistence.dao.ifina2100DAO;
import nhnis.infra.in.a.persistence.dao.ifina4200DAO;

@Service
public class ifina4200Service {
    private final ifina4200DAO dao;
    private final ifina1999DAO assetDao;
    private final ifina2100DAO systemDao;
    private final AuthGuard authGuard;

    public ifina4200Service(ifina4200DAO dao, ifina1999DAO assetDao, ifina2100DAO systemDao, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.assetDao = assetDao;
        this.systemDao = systemDao;
    }

    public ifina4200S0DTOout ifina4200S0(ifina4200S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "dbId", input.getDbId());
            put(param, "dbName", input.getDbName());
            put(param, "assetId", input.getAssetId());
            put(param, "systemId", input.getSystemId());
            put(param, "engineCd", input.getEngineCd());
            put(param, "statusCd", input.getStatusCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);
        int total = dao.ifina4200S0_S0_count(param);
        ifina4200S0DTOout out = new ifina4200S0DTOout();
        List<Map<String, Object>> rows = dao.ifina4200S0_S0(param);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                ifina4200S0DTOSub0 sub = new ifina4200S0DTOSub0();
                sub.setDbId(as(row, "DB_ID", "dbId"));
                sub.setDbName(as(row, "DB_NAME", "dbName"));
                sub.setEngineCd(as(row, "ENGINE_CD", "engineCd"));
                sub.setVersionNo(as(row, "VERSION_NO", "versionNo"));
                sub.setAssetId(as(row, "ASSET_ID", "assetId"));
                sub.setSystemId(as(row, "SYSTEM_ID", "systemId"));
                sub.setEolDate(as(row, "EOL_DATE", "eolDate"));
                sub.setStatusCd(as(row, "STATUS_CD", "statusCd"));
                sub.setRemark(as(row, "REMARK", "remark"));
                sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                out.addifina4200S0DTOSub0(sub);
            }
        }
        out.setSize(out.sizeifina4200S0DTOSub0());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina4200C0DTOout ifina4200C0(ifina4200C0DTOin input) throws Exception {
        ifina4200C0DTOout out = new ifina4200C0DTOout();
        if (authGuard.denyIfHard(out, "ifina4200C0")) return out;
        String dbId = trim(input == null ? null : input.getDbId());
        String dbName = trim(input == null ? null : input.getDbName());
        if (dbId == null || dbName == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: dbId, dbName");
            return out;
        }
        String assetId = trim(input.getAssetId());
        if (assetId != null && assetDao.ifina1999S0_S0_exists(Map.of("serverId", assetId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0004");
            out.setRSLT_MSG("자산 없음: " + assetId);
            return out;
        }
        String systemId = trim(input.getSystemId());
        if (systemId != null && systemDao.ifina2100S0_S0_exists(Map.of("systemId", systemId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0004");
            out.setRSLT_MSG("시스템 없음: " + systemId);
            return out;
        }
        if (dao.ifina4200S0_S0_exists(Map.of("dbId", dbId)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_DB_ID");
            return out;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("dbId", dbId);
        p.put("dbName", dbName);
        p.put("engineCd", blank(input.getEngineCd(), "ORACLE"));
        p.put("versionNo", empty(input.getVersionNo()));
        p.put("assetId", empty(input.getAssetId()));
        p.put("systemId", empty(input.getSystemId()));
        p.put("eolDate", empty(input.getEolDate()));
        p.put("statusCd", blank(input.getStatusCd(), "DISCOVERED"));
        p.put("remark", empty(input.getRemark()));
        p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
        p.put("regDtm", now());
        p.put("chgUserId", null);
        p.put("chgDtm", null);
        out.setPROC_CNT(dao.ifina4200C0_C0(p));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina4200U0DTOout ifina4200U0(ifina4200U0DTOin input) throws Exception {
        ifina4200U0DTOout out = new ifina4200U0DTOout();
        if (authGuard.denyIfHard(out, "ifina4200U0")) return out;
        String dbId = trim(input == null ? null : input.getDbId());
        String dbName = trim(input == null ? null : input.getDbName());
        if (dbId == null || dbName == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: dbId, dbName");
            return out;
        }
        if (dao.ifina4200S0_S0_exists(Map.of("dbId", dbId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        String assetId = trim(input.getAssetId());
        if (assetId != null && assetDao.ifina1999S0_S0_exists(Map.of("serverId", assetId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0004");
            out.setRSLT_MSG("자산 없음: " + assetId);
            return out;
        }
        String systemId = trim(input.getSystemId());
        if (systemId != null && systemDao.ifina2100S0_S0_exists(Map.of("systemId", systemId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0004");
            out.setRSLT_MSG("시스템 없음: " + systemId);
            return out;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("dbId", dbId);
        p.put("dbName", dbName);
        p.put("engineCd", empty(input.getEngineCd()));
        p.put("versionNo", empty(input.getVersionNo()));
        p.put("assetId", empty(input.getAssetId()));
        p.put("systemId", empty(input.getSystemId()));
        p.put("eolDate", empty(input.getEolDate()));
        p.put("statusCd", empty(input.getStatusCd()));
        p.put("remark", empty(input.getRemark()));
        p.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
        p.put("chgDtm", now());
        out.setPROC_CNT(dao.ifina4200U0_U0(p));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina4200D0DTOout ifina4200D0(ifina4200D0DTOin input) throws Exception {
        ifina4200D0DTOout out = new ifina4200D0DTOout();
        if (authGuard.denyIfHard(out, "ifina4200D0")) return out;
        if (input == null || input.getDbIdList() == null || input.getDbIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getDbIdList().stream()
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
        out.setPROC_CNT(dao.ifina4200D0_D0(Map.of("dbIdList", ids)));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static int pageNo(Integer v) {
        return v == null || v <= 0 ? 1 : v;
    }

    private static int pageSize(Integer v) {
        int s = v == null || v <= 0 ? 10 : v;
        return Math.min(s, 100);
    }

    private static String now() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
    }

    private static String trim(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String empty(String v) {
        return v == null ? "" : v.trim();
    }

    private static String blank(String v, String d) {
        String t = trim(v);
        return t != null ? t : d;
    }

    private static void put(Map<String, Object> m, String k, String v) {
        if (v != null && !v.isBlank()) {
            m.put(k, v.trim());
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
                if (e.getKey() != null
                        && (e.getKey().equalsIgnoreCase(u) || e.getKey().equalsIgnoreCase(c))) {
                    v = e.getValue();
                    break;
                }
            }
        }
        return v == null ? null : String.valueOf(v);
    }
}
