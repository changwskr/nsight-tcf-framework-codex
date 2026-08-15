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

import nhnis.infra.in.a.dto.ifina1200C0DTOin;
import nhnis.infra.in.a.dto.ifina1200C0DTOout;
import nhnis.infra.in.a.dto.ifina1200D0DTOin;
import nhnis.infra.in.a.dto.ifina1200D0DTOout;
import nhnis.infra.in.a.dto.ifina1200S0DTOSub0;
import nhnis.infra.in.a.dto.ifina1200S0DTOin;
import nhnis.infra.in.a.dto.ifina1200S0DTOout;
import nhnis.infra.in.a.dto.ifina1200U0DTOin;
import nhnis.infra.in.a.dto.ifina1200U0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina1200DAO;

@Service
public class ifina1200Service {
    private final ifina1200DAO dao;
    private final AuthGuard authGuard;

    public ifina1200Service(ifina1200DAO dao, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
    }

    public ifina1200S0DTOout ifina1200S0(ifina1200S0DTOin input) throws Exception {
        String entityType = blank(input == null ? null : input.getEntityType(), "ITEM").toUpperCase(Locale.ROOT);
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "templateId", input.getTemplateId());
            put(param, "itemId", input.getItemId());
            put(param, "techRoleCd", input.getTechRoleCd());
            put(param, "activeYn", input.getActiveYn());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        ifina1200S0DTOout out = new ifina1200S0DTOout();
        // always attach template catalog for UI left panel
        Map<String, Object> tmplParam = new HashMap<>();
        tmplParam.put("offset", 0);
        tmplParam.put("pageSize", 200);
        put(tmplParam, "activeYn", input == null ? null : input.getActiveYn());
        List<Map<String, Object>> tmplRows = dao.ifina1200S0_tmpl(tmplParam);
        List<Map<String, Object>> templates = new ArrayList<>();
        if (tmplRows != null) {
            for (Map<String, Object> row : tmplRows) {
                Map<String, Object> m = new HashMap<>();
                m.put("templateId", as(row, "TEMPLATE_ID", "templateId"));
                m.put("templateName", as(row, "TEMPLATE_NAME", "templateName"));
                m.put("techRoleCd", as(row, "TECH_ROLE_CD", "techRoleCd"));
                m.put("activeYn", as(row, "ACTIVE_YN", "activeYn"));
                templates.add(m);
            }
        }
        out.setTemplates(templates);

        int total;
        if ("TEMPLATE".equals(entityType)) {
            total = dao.ifina1200S0_tmpl_count(param);
            List<Map<String, Object>> rows = dao.ifina1200S0_tmpl(param);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    ifina1200S0DTOSub0 sub = new ifina1200S0DTOSub0();
                    sub.setEntityType("TEMPLATE");
                    sub.setTemplateId(as(row, "TEMPLATE_ID", "templateId"));
                    sub.setTemplateName(as(row, "TEMPLATE_NAME", "templateName"));
                    sub.setTechRoleCd(as(row, "TECH_ROLE_CD", "techRoleCd"));
                    sub.setActiveYn(as(row, "ACTIVE_YN", "activeYn"));
                    sub.setRemark(as(row, "REMARK", "remark"));
                    sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                    sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                    sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                    sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                    out.addifina1200S0DTOSub0(sub);
                }
            }
        } else {
            total = dao.ifina1200S0_item_count(param);
            List<Map<String, Object>> rows = dao.ifina1200S0_item(param);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    ifina1200S0DTOSub0 sub = new ifina1200S0DTOSub0();
                    sub.setEntityType("ITEM");
                    sub.setTemplateId(as(row, "TEMPLATE_ID", "templateId"));
                    sub.setTemplateName(as(row, "TEMPLATE_NAME", "templateName"));
                    sub.setItemId(as(row, "ITEM_ID", "itemId"));
                    sub.setItemName(as(row, "ITEM_NAME", "itemName"));
                    sub.setItemTypeCd(as(row, "ITEM_TYPE_CD", "itemTypeCd"));
                    sub.setRequiredYn(as(row, "REQUIRED_YN", "requiredYn"));
                    sub.setSortNo(asInt(row, "SORT_NO", "sortNo"));
                    sub.setActiveYn(as(row, "ACTIVE_YN", "activeYn"));
                    sub.setRemark(as(row, "REMARK", "remark"));
                    sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                    sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                    sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                    sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                    out.addifina1200S0DTOSub0(sub);
                }
            }
        }
        out.setSize(out.sizeifina1200S0DTOSub0());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina1200C0DTOout ifina1200C0(ifina1200C0DTOin input) throws Exception {
        ifina1200C0DTOout out = new ifina1200C0DTOout();
        if (authGuard.denyIfHard(out, "ifina1200C0")) return out;
        String entityType = blank(input == null ? null : input.getEntityType(), "ITEM").toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(entityType)) {
            String templateId = trim(input.getTemplateId());
            String templateName = trim(input.getTemplateName());
            if (templateId == null || templateName == null) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: templateId, templateName");
                return out;
            }
            if (dao.ifina1200S0_tmpl_exists(Map.of("templateId", templateId)) > 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0002");
                out.setRSLT_MSG("DUPLICATE_TEMPLATE_ID");
                return out;
            }
            Map<String, Object> p = new HashMap<>();
            p.put("templateId", templateId);
            p.put("templateName", templateName);
            p.put("techRoleCd", empty(input.getTechRoleCd()));
            p.put("activeYn", blank(input.getActiveYn(), "Y"));
            p.put("remark", empty(input.getRemark()));
            p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
            p.put("regDtm", now());
            p.put("chgUserId", null);
            p.put("chgDtm", null);
            out.setPROC_CNT(dao.ifina1200C0_tmpl(p));
        } else {
            String templateId = trim(input.getTemplateId());
            String itemId = trim(input.getItemId());
            String itemName = trim(input.getItemName());
            if (templateId == null || itemId == null || itemName == null) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: templateId, itemId, itemName");
                return out;
            }
            if (dao.ifina1200S0_tmpl_exists(Map.of("templateId", templateId)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0004");
                out.setRSLT_MSG("템플릿 없음: " + templateId);
                return out;
            }
            if (dao.ifina1200S0_item_exists(Map.of("templateId", templateId, "itemId", itemId)) > 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0002");
                out.setRSLT_MSG("DUPLICATE_ITEM_ID");
                return out;
            }
            Map<String, Object> p = new HashMap<>();
            p.put("templateId", templateId);
            p.put("itemId", itemId);
            p.put("itemName", itemName);
            p.put("itemTypeCd", blank(input.getItemTypeCd(), "TEXT"));
            p.put("requiredYn", blank(input.getRequiredYn(), "N"));
            p.put("sortNo", input.getSortNo() == null ? 0 : input.getSortNo());
            p.put("activeYn", blank(input.getActiveYn(), "Y"));
            p.put("remark", empty(input.getRemark()));
            p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
            p.put("regDtm", now());
            p.put("chgUserId", null);
            p.put("chgDtm", null);
            out.setPROC_CNT(dao.ifina1200C0_item(p));
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina1200U0DTOout ifina1200U0(ifina1200U0DTOin input) throws Exception {
        ifina1200U0DTOout out = new ifina1200U0DTOout();
        if (authGuard.denyIfHard(out, "ifina1200U0")) return out;
        String entityType = blank(input == null ? null : input.getEntityType(), "ITEM").toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(entityType)) {
            String templateId = trim(input.getTemplateId());
            String templateName = trim(input.getTemplateName());
            if (templateId == null || templateName == null) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: templateId, templateName");
                return out;
            }
            if (dao.ifina1200S0_tmpl_exists(Map.of("templateId", templateId)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0003");
                out.setRSLT_MSG("NOT_FOUND");
                return out;
            }
            Map<String, Object> p = new HashMap<>();
            p.put("templateId", templateId);
            p.put("templateName", templateName);
            p.put("techRoleCd", empty(input.getTechRoleCd()));
            p.put("activeYn", blank(input.getActiveYn(), "Y"));
            p.put("remark", empty(input.getRemark()));
            p.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
            p.put("chgDtm", now());
            out.setPROC_CNT(dao.ifina1200U0_tmpl(p));
        } else {
            String templateId = trim(input.getTemplateId());
            String itemId = trim(input.getItemId());
            String itemName = trim(input.getItemName());
            if (templateId == null || itemId == null || itemName == null) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: templateId, itemId, itemName");
                return out;
            }
            if (dao.ifina1200S0_item_exists(Map.of("templateId", templateId, "itemId", itemId)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0003");
                out.setRSLT_MSG("NOT_FOUND");
                return out;
            }
            Map<String, Object> p = new HashMap<>();
            p.put("templateId", templateId);
            p.put("itemId", itemId);
            p.put("itemName", itemName);
            p.put("itemTypeCd", empty(input.getItemTypeCd()));
            p.put("requiredYn", blank(input.getRequiredYn(), "N"));
            p.put("sortNo", input.getSortNo() == null ? 0 : input.getSortNo());
            p.put("activeYn", blank(input.getActiveYn(), "Y"));
            p.put("remark", empty(input.getRemark()));
            p.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
            p.put("chgDtm", now());
            out.setPROC_CNT(dao.ifina1200U0_item(p));
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina1200D0DTOout ifina1200D0(ifina1200D0DTOin input) throws Exception {
        ifina1200D0DTOout out = new ifina1200D0DTOout();
        if (authGuard.denyIfHard(out, "ifina1200D0")) return out;
        String entityType = blank(input == null ? null : input.getEntityType(), "ITEM").toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(entityType)) {
            if (input == null || input.getTemplateIdList() == null || input.getTemplateIdList().isEmpty()) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("NO_DATA");
                return out;
            }
            List<String> ids = input.getTemplateIdList().stream()
                    .filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
            Map<String, Object> p = Map.of("templateIdList", ids);
            dao.ifina1200D0_item_by_tmpl(p);
            out.setPROC_CNT(dao.ifina1200D0_tmpl(p));
        } else {
            String templateId = trim(input == null ? null : input.getTemplateId());
            if (templateId == null || input.getItemIdList() == null || input.getItemIdList().isEmpty()) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: templateId, itemIdList");
                return out;
            }
            List<String> ids = input.getItemIdList().stream()
                    .filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
            Map<String, Object> p = new HashMap<>();
            p.put("templateId", templateId);
            p.put("itemIdList", ids);
            out.setPROC_CNT(dao.ifina1200D0_item(p));
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static int pageNo(Integer v) {
        return v == null || v <= 0 ? 1 : v;
    }

    private static int pageSize(Integer v) {
        int s = v == null || v <= 0 ? 50 : v;
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
