package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.dto.ifina3100C0DTOin;
import nhnis.infra.in.a.dto.ifina3100C0DTOout;
import nhnis.infra.in.a.dto.ifina3100D0DTOin;
import nhnis.infra.in.a.dto.ifina3100D0DTOout;
import nhnis.infra.in.a.dto.ifina3100S0DTOSub0;
import nhnis.infra.in.a.dto.ifina3100S0DTOin;
import nhnis.infra.in.a.dto.ifina3100S0DTOout;
import nhnis.infra.in.a.dto.ifina3100S1DTOin;
import nhnis.infra.in.a.dto.ifina3100S1DTOout;
import nhnis.infra.in.a.dto.ifina3100U0DTOin;
import nhnis.infra.in.a.dto.ifina3100U0DTOout;
import nhnis.infra.in.a.application.rule.LifecycleTransitionRule;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.InfraIntegrityValidator;
import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.persistence.dao.ifina3100DAO;

@Service
public class ifina3100Service {

    private final ifina3100DAO dao;
    private final InfraIntegrityValidator validator;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;
    private final LifecycleTransitionRule lifecycleRule;

    public ifina3100Service(
            ifina3100DAO dao,
            InfraIntegrityValidator validator,
            ChangeLogWriter changeLogWriter,
            AuthGuard authGuard,
            LifecycleTransitionRule lifecycleRule) {
        this.dao = dao;
        this.validator = validator;
        this.changeLogWriter = changeLogWriter;
        this.authGuard = authGuard;
        this.lifecycleRule = lifecycleRule;
    }

    public ifina3100S0DTOout ifina3100S0(ifina3100S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "assetId", input.getAssetId());
            put(param, "assetName", input.getAssetName());
            put(param, "systemId", input.getSystemId());
            put(param, "groupId", input.getGroupId());
            put(param, "assetKindCd", input.getAssetKindCd());
            put(param, "techRoleCd", input.getTechRoleCd());
            put(param, "envCd", input.getEnvCd());
            put(param, "statusCd", input.getStatusCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        int total = dao.ifina3100S0_S0_count(param);
        ifina3100S0DTOout out = new ifina3100S0DTOout();
        List<Map<String, Object>> rows = dao.ifina3100S0_S0(param);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                out.addifina3100S0DTOSub0(mapSub(row));
            }
        }
        out.setSize(out.sizeifina3100S0DTOSub0());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina3100S1DTOout ifina3100S1(ifina3100S1DTOin input) throws Exception {
        ifina3100S1DTOout out = new ifina3100S1DTOout();
        String assetId = trim(input == null ? null : input.getAssetId());
        if (assetId == null) {
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: assetId");
            return out;
        }
        Map<String, Object> row = dao.ifina3100S1_S1(Map.of("assetId", assetId));
        if (row == null || row.isEmpty()) {
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        ifina3100S0DTOSub0 base = mapSub(row);
        out.setBase(base);
        Map<String, Object> id = Map.of("assetId", assetId);
        List<Map<String, Object>> eps = dao.ifina3100S1_endpoints(id);
        List<Map<String, Object>> endpoints = new ArrayList<>();
        if (eps != null) {
            for (Map<String, Object> ep : eps) {
                Map<String, Object> m = new HashMap<>();
                m.put("endpointId", as(ep, "ENDPOINT_ID", "endpointId"));
                m.put("endpointTypeCd", as(ep, "ENDPOINT_TYPE_CD", "endpointTypeCd"));
                m.put("address", as(ep, "ADDRESS", "address"));
                m.put("portNo", as(ep, "PORT_NO", "portNo"));
                m.put("protocolCd", as(ep, "PROTOCOL_CD", "protocolCd"));
                m.put("primaryYn", as(ep, "PRIMARY_YN", "primaryYn"));
                endpoints.add(m);
            }
        }
        out.setEndpoints(endpoints);
        out.setEndpointCount(endpoints.size());
        out.setMwCount(dao.ifina3100S1_mw_count(id));
        out.setDbCount(dao.ifina3100S1_db_count(id));

        List<Map<String, Object>> attrs = loadAttrs(assetId, base.getTechRoleCd());
        out.setAttrs(attrs);
        out.setAttrCount(attrs.size());

        List<String> warnings = new ArrayList<>();
        if ("SAAS".equalsIgnoreCase(blank(base.getAssetKindCd(), ""))
                || "CLOUD_SVC".equalsIgnoreCase(blank(base.getAssetKindCd(), ""))) {
            warnings.add("RL-AS-003: SaaS/Cloud — Compute 입력 비권장");
        }
        if (endpoints.isEmpty()) {
            warnings.add("RL-NT: Endpoint 미등록");
        }
        warnings.addAll(requiredAttrWarnings(attrs));
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifina3100C0DTOout ifina3100C0(ifina3100C0DTOin input) throws Exception {
        ifina3100C0DTOout out = new ifina3100C0DTOout();
        String techRole = input == null ? null : input.getTechRoleCd();
        if (authGuard.denyIfHard(out, "ifina3100C0", techRoleAttrs(techRole))) return out;
        ValidationResult vr = validator.validateAssetCreate(
                input == null ? null : input.getAssetId(),
                input == null ? null : input.getAssetName(),
                input == null ? null : input.getAssetKindCd(),
                input == null ? null : input.getEnvCd(),
                input == null ? null : input.getTechRoleCd(),
                input == null ? null : input.getSystemId(),
                input == null ? null : input.getGroupId());
        if (vr.hasHard()) {
            RuleViolation h = vr.firstHard().orElseThrow();
            out.setPROC_CNT(0);
            out.setRSLT_CD(h.getRsltCd());
            out.setRSLT_MSG(h.formatted());
            return out;
        }
        Map<String, Object> p = baseMap(input, true);
        out.setPROC_CNT(dao.ifina3100C0_C0(p));
        List<String> attrWarn = saveAttrs(String.valueOf(p.get("assetId")),
                input == null ? null : input.getTechRoleCd(),
                input == null ? null : input.getAttrs());
        changeLogWriter.write("ASSET", String.valueOf(p.get("assetId")), "CREATE", null, p, "ifina3100C0");
        List<String> msgs = new ArrayList<>(vr.softWarnings());
        msgs.addAll(attrWarn);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(msgs.isEmpty() ? "OK" : String.join("; ", msgs));
        return out;
    }

    public ifina3100U0DTOout ifina3100U0(ifina3100U0DTOin input) throws Exception {
        ifina3100U0DTOout out = new ifina3100U0DTOout();
        String assetId = input == null ? null : trim(input.getAssetId());
        Map<String, Object> before = assetId == null ? null : dao.ifina3100S1_S1(Map.of("assetId", assetId));
        String techRole = input == null ? null : input.getTechRoleCd();
        if (techRole == null || techRole.isBlank()) {
            techRole = as(before, "TECH_ROLE_CD", "techRoleCd");
        }
        if (authGuard.denyIfHard(out, "ifina3100U0", techRoleAttrs(techRole))) return out;
        ValidationResult vr = validator.validateAssetUpdate(
                input == null ? null : input.getAssetId(),
                input == null ? null : input.getAssetName(),
                input == null ? null : input.getAssetKindCd(),
                input == null ? null : input.getEnvCd(),
                input == null ? null : input.getTechRoleCd(),
                input == null ? null : input.getSystemId(),
                input == null ? null : input.getGroupId());
        if (vr.hasHard()) {
            RuleViolation h = vr.firstHard().orElseThrow();
            out.setPROC_CNT(0);
            out.setRSLT_CD(h.getRsltCd());
            out.setRSLT_MSG(h.formatted());
            return out;
        }
        String fromStatus = before == null ? null : as(before, "STATUS_CD", "statusCd");
        String toStatus = trim(input.getStatusCd());
        if (toStatus == null || toStatus.isBlank()) {
            toStatus = fromStatus;
        }
        ValidationResult life = lifecycleRule.evaluate(fromStatus, toStatus);
        if (life.hasHard()) {
            RuleViolation h = life.firstHard().orElseThrow();
            out.setPROC_CNT(0);
            out.setRSLT_CD(h.getRsltCd());
            out.setRSLT_MSG(h.formatted());
            return out;
        }
        vr.addAll(life);
        Map<String, Object> p = baseMap(input, false);
        out.setPROC_CNT(dao.ifina3100U0_U0(p));
        List<String> attrWarn = saveAttrs(String.valueOf(p.get("assetId")),
                input.getTechRoleCd() != null ? input.getTechRoleCd() : as(before, "TECH_ROLE_CD", "techRoleCd"),
                input.getAttrs());
        changeLogWriter.write("ASSET", String.valueOf(p.get("assetId")), "UPDATE", before, p, "ifina3100U0");
        List<String> msgs = new ArrayList<>(vr.softWarnings());
        msgs.addAll(attrWarn);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(msgs.isEmpty() ? "OK" : String.join("; ", msgs));
        return out;
    }

    public ifina3100D0DTOout ifina3100D0(ifina3100D0DTOin input) throws Exception {
        ifina3100D0DTOout out = new ifina3100D0DTOout();
        if (input == null || input.getAssetIdList() == null || input.getAssetIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getAssetIdList().stream()
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
        for (String id : ids) {
            Map<String, Object> row = dao.ifina3100S1_S1(Map.of("assetId", id));
            String techRole = as(row, "TECH_ROLE_CD", "techRoleCd");
            if (authGuard.denyIfHard(out, "ifina3100D0", techRoleAttrs(techRole))) {
                return out;
            }
        }
        boolean hardDelete = "Y".equalsIgnoreCase(blank(input.getHardDeleteYn(), "N"));
        if (!hardDelete) {
            for (String id : ids) {
                Map<String, Object> before = dao.ifina3100S1_S1(Map.of("assetId", id));
                String fromStatus = before == null ? null : as(before, "STATUS_CD", "statusCd");
                ValidationResult life = lifecycleRule.evaluate(fromStatus, "RETIRED", true);
                if (life.hasHard()) {
                    RuleViolation h = life.firstHard().orElseThrow();
                    out.setPROC_CNT(0);
                    out.setRSLT_CD(h.getRsltCd());
                    out.setRSLT_MSG(h.formatted() + " assetId=" + id);
                    return out;
                }
            }
        }
        Map<String, Object> p = new HashMap<>();
        p.put("assetIdList", ids);
        p.put("chgUserId", "LOCAL");
        p.put("chgDtm", now());
        int cnt;
        String action;
        if (hardDelete) {
            dao.ifina3100D0_attr_delete(p);
            cnt = dao.ifina3100D0_D0(p);
            action = "DELETE";
        } else {
            cnt = dao.ifina3100D0_retire(p);
            action = "STATUS";
        }
        for (String id : ids) {
            changeLogWriter.write("ASSET", id, action, null, Map.of("statusCd", "RETIRED"), "ifina3100D0");
        }
        out.setPROC_CNT(cnt);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private Map<String, Object> baseMap(Object input, boolean create) {
        Map<String, Object> p = new HashMap<>();
        if (input instanceof ifina3100C0DTOin in) {
            p.put("assetId", empty(in.getAssetId()));
            p.put("assetName", empty(in.getAssetName()));
            p.put("groupId", empty(in.getGroupId()));
            p.put("systemId", empty(in.getSystemId()));
            p.put("assetKindCd", blank(in.getAssetKindCd(), "VM"));
            p.put("techRoleCd", empty(in.getTechRoleCd()));
            p.put("envCd", blank(in.getEnvCd(), "DEV"));
            p.put("tierCd", blank(in.getTierCd(), "TIER3"));
            p.put("serviceModelCd", blank(in.getServiceModelCd(), "IAAS"));
            p.put("deployModelCd", blank(in.getDeployModelCd(), "ON_PREMISE"));
            p.put("statusCd", blank(in.getStatusCd(), "DISCOVERED"));
            p.put("osName", empty(in.getOsName()));
            p.put("osVersion", empty(in.getOsVersion()));
            p.put("osEolDate", empty(in.getOsEolDate()));
            p.put("remark", empty(in.getRemark()));
            p.put("regUserId", blank(in.getRegUserId(), "LOCAL"));
            p.put("regDtm", now());
            p.put("chgUserId", null);
            p.put("chgDtm", null);
        } else if (input instanceof ifina3100U0DTOin in) {
            p.put("assetId", empty(in.getAssetId()));
            p.put("assetName", empty(in.getAssetName()));
            p.put("groupId", empty(in.getGroupId()));
            p.put("systemId", empty(in.getSystemId()));
            p.put("assetKindCd", empty(in.getAssetKindCd()));
            p.put("techRoleCd", empty(in.getTechRoleCd()));
            p.put("envCd", empty(in.getEnvCd()));
            p.put("tierCd", empty(in.getTierCd()));
            p.put("serviceModelCd", empty(in.getServiceModelCd()));
            p.put("deployModelCd", empty(in.getDeployModelCd()));
            p.put("statusCd", empty(in.getStatusCd()));
            p.put("osName", empty(in.getOsName()));
            p.put("osVersion", empty(in.getOsVersion()));
            p.put("osEolDate", empty(in.getOsEolDate()));
            p.put("remark", empty(in.getRemark()));
            p.put("chgUserId", blank(in.getChgUserId(), "LOCAL"));
            p.put("chgDtm", now());
        }
        return p;
    }

    private List<Map<String, Object>> loadAttrs(String assetId, String techRoleCd) throws Exception {
        List<String> templates = templatesFor(techRoleCd);
        Map<String, Object> q = new HashMap<>();
        q.put("assetId", assetId);
        q.put("templateIdList", templates);
        List<Map<String, Object>> raw = dao.ifina3100S1_attrs(q);
        List<Map<String, Object>> attrs = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("templateId", as(row, "TEMPLATE_ID", "templateId"));
                m.put("itemId", as(row, "ITEM_ID", "itemId"));
                m.put("itemName", as(row, "ITEM_NAME", "itemName"));
                m.put("itemTypeCd", as(row, "ITEM_TYPE_CD", "itemTypeCd"));
                m.put("requiredYn", blank(as(row, "REQUIRED_YN", "requiredYn"), "N"));
                m.put("sortNo", as(row, "SORT_NO", "sortNo"));
                m.put("attrValue", as(row, "ATTR_VALUE", "attrValue"));
                attrs.add(m);
            }
        }
        return attrs;
    }

    private List<String> saveAttrs(String assetId, String techRoleCd, List<Map<String, Object>> incoming) throws Exception {
        if (incoming == null || incoming.isEmpty()) {
            return requiredAttrWarnings(loadAttrs(assetId, techRoleCd));
        }
        String dtm = now();
        for (Map<String, Object> row : incoming) {
            if (row == null) {
                continue;
            }
            String itemId = str(row, "itemId", "ITEM_ID");
            if (itemId == null) {
                continue;
            }
            String value = blank(str(row, "attrValue", "ATTR_VALUE"), "");
            Map<String, Object> p = new HashMap<>();
            p.put("assetId", assetId);
            p.put("itemId", itemId);
            p.put("attrValue", value);
            p.put("updatedAt", dtm);
            if (dao.ifina3100U0_attr_exists(p) > 0) {
                dao.ifina3100U0_attr_update(p);
            } else {
                dao.ifina3100U0_attr_insert(p);
            }
        }
        return requiredAttrWarnings(loadAttrs(assetId, techRoleCd));
    }

    private static List<String> requiredAttrWarnings(List<Map<String, Object>> attrs) {
        List<String> w = new ArrayList<>();
        for (Map<String, Object> a : attrs) {
            if (!"Y".equalsIgnoreCase(String.valueOf(a.get("requiredYn")))) {
                continue;
            }
            Object v = a.get("attrValue");
            if (v == null || String.valueOf(v).isBlank()) {
                w.add("RL-UP-ATTR: 필수 확장항목 미입력 " + a.get("itemId") + "(" + a.get("itemName") + ")");
            }
        }
        return w;
    }

    private static List<String> templatesFor(String techRoleCd) {
        List<String> t = new ArrayList<>();
        t.add("TMPL_COMMON");
        String role = techRoleCd == null ? "" : techRoleCd.trim().toUpperCase(Locale.ROOT);
        if ("WAS".equals(role) || "APP".equals(role)) {
            t.add("TMPL_WAS");
        } else if ("WEB".equals(role)) {
            t.add("TMPL_WEB");
        } else if ("DATABASE".equals(role) || "DB".equals(role) || "RDBMS".equals(role)) {
            t.add("TMPL_RDBMS");
        } else if ("BARE_METAL".equals(role)) {
            t.add("TMPL_BARE_METAL");
        }
        return t;
    }

    private static Map<String, String> techRoleAttrs(String techRoleCd) {
        if (techRoleCd == null || techRoleCd.isBlank()) {
            return Map.of();
        }
        return Map.of("techRoleCd", techRoleCd.trim());
    }

    private static String str(Map<String, Object> row, String... keys) {
        if (row == null) {
            return null;
        }
        for (String k : keys) {
            Object v = row.get(k);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v).trim();
            }
        }
        return null;
    }

    private ifina3100S0DTOSub0 mapSub(Map<String, Object> row) {
        ifina3100S0DTOSub0 sub = new ifina3100S0DTOSub0();
        sub.setAssetId(as(row, "ASSET_ID", "assetId"));
        sub.setAssetName(as(row, "ASSET_NAME", "assetName"));
        sub.setGroupId(as(row, "GROUP_ID", "groupId"));
        sub.setSystemId(as(row, "SYSTEM_ID", "systemId"));
        sub.setAssetKindCd(as(row, "ASSET_KIND_CD", "assetKindCd"));
        sub.setTechRoleCd(as(row, "TECH_ROLE_CD", "techRoleCd"));
        sub.setEnvCd(as(row, "ENV_CD", "envCd"));
        sub.setTierCd(as(row, "TIER_CD", "tierCd"));
        sub.setServiceModelCd(as(row, "SERVICE_MODEL_CD", "serviceModelCd"));
        sub.setDeployModelCd(as(row, "DEPLOY_MODEL_CD", "deployModelCd"));
        sub.setStatusCd(as(row, "STATUS_CD", "statusCd"));
        sub.setOsName(as(row, "OS_NAME", "osName"));
        sub.setOsVersion(as(row, "OS_VERSION", "osVersion"));
        sub.setOsEolDate(as(row, "OS_EOL_DATE", "osEolDate"));
        sub.setRemark(as(row, "REMARK", "remark"));
        sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
        sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
        sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
        sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
        return sub;
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
