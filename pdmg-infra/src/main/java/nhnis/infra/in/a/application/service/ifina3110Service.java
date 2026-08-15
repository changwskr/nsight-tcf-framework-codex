package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.persistence.dao.ifina2100DAO;
import nhnis.infra.in.a.persistence.dao.ifina3110DAO;
import nhnis.infra.in.a.dto.ifina3110C0DTOin;
import nhnis.infra.in.a.dto.ifina3110C0DTOout;
import nhnis.infra.in.a.dto.ifina3110D0DTOin;
import nhnis.infra.in.a.dto.ifina3110D0DTOout;
import nhnis.infra.in.a.dto.ifina3110S0DTOSub0;
import nhnis.infra.in.a.dto.ifina3110S0DTOin;
import nhnis.infra.in.a.dto.ifina3110S0DTOout;
import nhnis.infra.in.a.dto.ifina3110U0DTOin;
import nhnis.infra.in.a.dto.ifina3110U0DTOout;

@Service
public class ifina3110Service {

    private final ifina3110DAO ifina3110DAO;
    private final ifina2100DAO ifina2100DAO;
    private final AuthGuard authGuard;

    public ifina3110Service(ifina3110DAO ifina3110DAO, ifina2100DAO ifina2100DAO, AuthGuard authGuard) {
        this.ifina3110DAO = ifina3110DAO;
        this.ifina2100DAO = ifina2100DAO;
        this.authGuard = authGuard;
    }

    public ifina3110S0DTOout ifina3110S0(ifina3110S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "groupId", input.getGroupId());
            put(param, "groupName", input.getGroupName());
            put(param, "systemId", input.getSystemId());
            put(param, "techRoleCd", input.getTechRoleCd());
            put(param, "envCd", input.getEnvCd());
            put(param, "tierCd", input.getTierCd());
            put(param, "statusCd", input.getStatusCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("pageNo", pageNo);
        param.put("pageSize", pageSize);
        param.put("offset", (pageNo - 1) * pageSize);

        int total = ifina3110DAO.ifina3110S0_S0_count(param);
        List<Map<String, Object>> rows = ifina3110DAO.ifina3110S0_S0(param);
        ifina3110S0DTOout out = new ifina3110S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                ifina3110S0DTOSub0 sub = new ifina3110S0DTOSub0();
                sub.setGroupId(as(row, "GROUP_ID", "groupId"));
                sub.setGroupName(as(row, "GROUP_NAME", "groupName"));
                sub.setSystemId(as(row, "SYSTEM_ID", "systemId"));
                sub.setTechRoleCd(as(row, "TECH_ROLE_CD", "techRoleCd"));
                sub.setEnvCd(as(row, "ENV_CD", "envCd"));
                sub.setTierCd(as(row, "TIER_CD", "tierCd"));
                sub.setStatusCd(as(row, "STATUS_CD", "statusCd"));
                sub.setActiveNodes(asInt(row, "ACTIVE_NODES", "activeNodes"));
                sub.setStandbyNodes(asInt(row, "STANDBY_NODES", "standbyNodes"));
                sub.setDrNodes(asInt(row, "DR_NODES", "drNodes"));
                sub.setRemark(as(row, "REMARK", "remark"));
                sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                out.addifina3110S0DTOSub0(sub);
            }
        }
        out.setSize(out.sizeifina3110S0DTOSub0());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina3110C0DTOout ifina3110C0(ifina3110C0DTOin input) throws Exception {
        ifina3110C0DTOout out = new ifina3110C0DTOout();
        if (authGuard.denyIfHard(out, "ifina3110C0")) return out;
        String groupId = trim(input == null ? null : input.getGroupId());
        String groupName = trim(input == null ? null : input.getGroupName());
        String techRoleCd = trim(input == null ? null : input.getTechRoleCd());
        String envCd = trim(input == null ? null : input.getEnvCd());
        if (groupId == null || groupName == null || techRoleCd == null || envCd == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: groupId, groupName, techRoleCd, envCd");
            return out;
        }
        if (ifina3110DAO.ifina3110S0_S0_exists(Map.of("groupId", groupId)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_GROUP_ID");
            return out;
        }
        String systemId = trim(input.getSystemId());
        if (systemId != null && ifina2100DAO.ifina2100S0_S0_exists(Map.of("systemId", systemId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0004");
            out.setRSLT_MSG("[RL-AS-005] 시스템 없음: " + systemId);
            return out;
        }
        Map<String, Object> param = baseParam(input, groupId, groupName);
        param.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
        param.put("regDtm", now());
        int cnt = ifina3110DAO.ifina3110C0_C0(param);
        out.setPROC_CNT(cnt);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina3110U0DTOout ifina3110U0(ifina3110U0DTOin input) throws Exception {
        ifina3110U0DTOout out = new ifina3110U0DTOout();
        if (authGuard.denyIfHard(out, "ifina3110U0")) return out;
        String groupId = trim(input == null ? null : input.getGroupId());
        String groupName = trim(input == null ? null : input.getGroupName());
        String techRoleCd = trim(input == null ? null : input.getTechRoleCd());
        String envCd = trim(input == null ? null : input.getEnvCd());
        if (groupId == null || groupName == null || techRoleCd == null || envCd == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: groupId, groupName, techRoleCd, envCd");
            return out;
        }
        if (ifina3110DAO.ifina3110S0_S0_exists(Map.of("groupId", groupId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        String systemId = trim(input.getSystemId());
        if (systemId != null && ifina2100DAO.ifina2100S0_S0_exists(Map.of("systemId", systemId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0004");
            out.setRSLT_MSG("[RL-AS-005] 시스템 없음: " + systemId);
            return out;
        }
        Map<String, Object> param = baseParam(input, groupId, groupName);
        param.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
        param.put("chgDtm", now());
        int cnt = ifina3110DAO.ifina3110U0_U0(param);
        out.setPROC_CNT(cnt);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina3110D0DTOout ifina3110D0(ifina3110D0DTOin input) throws Exception {
        ifina3110D0DTOout out = new ifina3110D0DTOout();
        if (authGuard.denyIfHard(out, "ifina3110D0")) return out;
        if (input == null || input.getGroupIdList() == null || input.getGroupIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getGroupIdList().stream()
                .filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
        if (ids.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        int cnt = ifina3110DAO.ifina3110D0_D0(Map.of("groupIdList", ids));
        out.setPROC_CNT(cnt);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private Map<String, Object> baseParam(Object input, String groupId, String groupName) {
        Map<String, Object> param = new HashMap<>();
        param.put("groupId", groupId);
        param.put("groupName", groupName);
        if (input instanceof ifina3110C0DTOin in) {
            param.put("systemId", empty(in.getSystemId()));
            param.put("techRoleCd", empty(in.getTechRoleCd()));
            param.put("envCd", empty(in.getEnvCd()));
            param.put("tierCd", blank(in.getTierCd(), "TIER3"));
            param.put("statusCd", blank(in.getStatusCd(), "DISCOVERED"));
            param.put("activeNodes", nz(in.getActiveNodes()));
            param.put("standbyNodes", nz(in.getStandbyNodes()));
            param.put("drNodes", nz(in.getDrNodes()));
            param.put("remark", empty(in.getRemark()));
        } else if (input instanceof ifina3110U0DTOin in) {
            param.put("systemId", empty(in.getSystemId()));
            param.put("techRoleCd", empty(in.getTechRoleCd()));
            param.put("envCd", empty(in.getEnvCd()));
            param.put("tierCd", empty(in.getTierCd()));
            param.put("statusCd", empty(in.getStatusCd()));
            param.put("activeNodes", nz(in.getActiveNodes()));
            param.put("standbyNodes", nz(in.getStandbyNodes()));
            param.put("drNodes", nz(in.getDrNodes()));
            param.put("remark", empty(in.getRemark()));
        }
        return param;
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }
    private static int pageNo(Integer v) { return v == null || v <= 0 ? 1 : v; }
    private static int pageSize(Integer v) {
        int size = v == null || v <= 0 ? 10 : v;
        return Math.min(size, 100);
    }
    private static String now() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
    }
    private static String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) {
        String t = trim(v);
        return t != null ? t : d;
    }
    private static void put(Map<String, Object> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v.trim());
    }
    private static String as(Map<String, Object> row, String u, String c) {
        Object v = val(row, u, c);
        return v == null ? null : String.valueOf(v);
    }
    private static Integer asInt(Map<String, Object> row, String u, String c) {
        Object v = val(row, u, c);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }
    private static Object val(Map<String, Object> row, String u, String c) {
        if (row == null) return null;
        Object v = row.get(u);
        if (v == null) v = row.get(c);
        if (v == null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && (e.getKey().equalsIgnoreCase(u) || e.getKey().equalsIgnoreCase(c))) {
                    return e.getValue();
                }
            }
        }
        return v;
    }
}
