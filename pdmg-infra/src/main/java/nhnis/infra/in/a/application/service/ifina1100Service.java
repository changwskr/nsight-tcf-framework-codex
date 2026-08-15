package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;

import nhnis.infra.in.a.dto.ifina1100C0DTOin;
import nhnis.infra.in.a.dto.ifina1100C0DTOout;
import nhnis.infra.in.a.dto.ifina1100S0DTOSub0;
import nhnis.infra.in.a.dto.ifina1100S0DTOin;
import nhnis.infra.in.a.dto.ifina1100S0DTOout;
import nhnis.infra.in.a.dto.ifina1100U0DTOin;
import nhnis.infra.in.a.dto.ifina1100U0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina1100DAO;

@Service
public class ifina1100Service {
    private final ifina1100DAO dao;
    private final AuthGuard authGuard;

    public ifina1100Service(ifina1100DAO dao, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
    }

    public ifina1100S0DTOout ifina1100S0(ifina1100S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "codeSetId", input.getCodeSetId());
            put(param, "codeValue", input.getCodeValue());
            put(param, "activeYn", input.getActiveYn());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        ifina1100S0DTOout out = new ifina1100S0DTOout();
        List<Map<String, Object>> sets = dao.ifina1100S0_sets(Map.of());
        List<Map<String, Object>> codeSets = new ArrayList<>();
        if (sets != null) {
            for (Map<String, Object> row : sets) {
                Map<String, Object> m = new HashMap<>();
                m.put("codeSetId", as(row, "CODE_SET_ID", "codeSetId"));
                m.put("codeSetName", as(row, "CODE_SET_NAME", "codeSetName"));
                m.put("remark", as(row, "REMARK", "remark"));
                codeSets.add(m);
            }
        }
        out.setCodeSets(codeSets);

        int total = dao.ifina1100S0_S0_count(param);
        List<Map<String, Object>> rows = dao.ifina1100S0_S0(param);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                ifina1100S0DTOSub0 sub = new ifina1100S0DTOSub0();
                sub.setCodeSetId(as(row, "CODE_SET_ID", "codeSetId"));
                sub.setCodeSetName(as(row, "CODE_SET_NAME", "codeSetName"));
                sub.setCodeValue(as(row, "CODE_VALUE", "codeValue"));
                sub.setNameKo(as(row, "NAME_KO", "nameKo"));
                sub.setSortOrder(asInt(row, "SORT_ORDER", "sortOrder"));
                sub.setActiveYn(as(row, "ACTIVE_YN", "activeYn"));
                sub.setRemark(as(row, "REMARK", "remark"));
                sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                out.addifina1100S0DTOSub0(sub);
            }
        }
        out.setSize(out.sizeifina1100S0DTOSub0());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina1100C0DTOout ifina1100C0(ifina1100C0DTOin input) throws Exception {
        ifina1100C0DTOout out = new ifina1100C0DTOout();
        if (authGuard.denyIfHard(out, "ifina1100C0")) return out;
        String codeSetId = trim(input == null ? null : input.getCodeSetId());
        String codeValue = trim(input == null ? null : input.getCodeValue());
        String nameKo = trim(input == null ? null : input.getNameKo());
        if (codeSetId == null || codeValue == null || nameKo == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: codeSetId, codeValue, nameKo");
            return out;
        }
        if (dao.ifina1100S0_set_exists(Map.of("codeSetId", codeSetId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0004");
            out.setRSLT_MSG("코드셋 없음: " + codeSetId);
            return out;
        }
        if (dao.ifina1100S0_S0_exists(Map.of("codeSetId", codeSetId, "codeValue", codeValue)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_CODE_VALUE");
            return out;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("codeSetId", codeSetId);
        p.put("codeValue", codeValue);
        p.put("nameKo", nameKo);
        p.put("sortOrder", input.getSortOrder() == null ? 0 : input.getSortOrder());
        p.put("activeYn", blank(input.getActiveYn(), "Y"));
        p.put("remark", empty(input.getRemark()));
        p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
        p.put("regDtm", now());
        p.put("chgUserId", null);
        p.put("chgDtm", null);
        out.setPROC_CNT(dao.ifina1100C0_C0(p));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina1100U0DTOout ifina1100U0(ifina1100U0DTOin input) throws Exception {
        ifina1100U0DTOout out = new ifina1100U0DTOout();
        if (authGuard.denyIfHard(out, "ifina1100U0")) return out;
        String codeSetId = trim(input == null ? null : input.getCodeSetId());
        String codeValue = trim(input == null ? null : input.getCodeValue());
        String nameKo = trim(input == null ? null : input.getNameKo());
        if (codeSetId == null || codeValue == null || nameKo == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: codeSetId, codeValue, nameKo");
            return out;
        }
        if (dao.ifina1100S0_S0_exists(Map.of("codeSetId", codeSetId, "codeValue", codeValue)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("codeSetId", codeSetId);
        p.put("codeValue", codeValue);
        p.put("nameKo", nameKo);
        p.put("sortOrder", input.getSortOrder() == null ? 0 : input.getSortOrder());
        p.put("activeYn", blank(input.getActiveYn(), "Y"));
        p.put("remark", empty(input.getRemark()));
        p.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
        p.put("chgDtm", now());
        out.setPROC_CNT(dao.ifina1100U0_U0(p));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static int pageNo(Integer v) {
        return v == null || v <= 0 ? 1 : v;
    }

    private static int pageSize(Integer v) {
        int s = v == null || v <= 0 ? 20 : v;
        return Math.min(s, 200);
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

    private static Integer asInt(Map<String, Object> row, String u, String c) {
        String s = as(row, u, c);
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
