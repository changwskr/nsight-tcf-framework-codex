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

import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.IdpAuthProperties;

import nhnis.infra.in.a.dto.ifina1500C0DTOin;
import nhnis.infra.in.a.dto.ifina1500C0DTOout;
import nhnis.infra.in.a.dto.ifina1500E0DTOin;
import nhnis.infra.in.a.dto.ifina1500E0DTOout;
import nhnis.infra.in.a.dto.ifina1500S0DTOSub0;
import nhnis.infra.in.a.dto.ifina1500S0DTOin;
import nhnis.infra.in.a.dto.ifina1500S0DTOout;
import nhnis.infra.in.a.dto.ifina1500U0DTOin;
import nhnis.infra.in.a.dto.ifina1500U0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina1500DAO;

@Service
public class ifina1500Service {
    private final ifina1500DAO dao;
    private final AuthGuard authGuard;
    private final IdpAuthProperties idpAuth;
    private final ChangeLogWriter changeLogWriter;

    public ifina1500Service(
            ifina1500DAO dao,
            AuthGuard authGuard,
            IdpAuthProperties idpAuth,
            ChangeLogWriter changeLogWriter) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.idpAuth = idpAuth;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina1500S0DTOout ifina1500S0(ifina1500S0DTOin input) throws Exception {
        String entityType = blank(input == null ? null : input.getEntityType(), "PERSON").toUpperCase(Locale.ROOT);
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "orgId", input.getOrgId());
            put(param, "personId", input.getPersonId());
            put(param, "activeYn", input.getActiveYn());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        ifina1500S0DTOout out = new ifina1500S0DTOout();
        int total;
        List<Map<String, Object>> rows;
        if ("ORG".equals(entityType)) {
            total = dao.ifina1500S0_org_count(param);
            rows = dao.ifina1500S0_org(param);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    ifina1500S0DTOSub0 sub = new ifina1500S0DTOSub0();
                    sub.setEntityType("ORG");
                    sub.setOrgId(as(row, "ORG_ID", "orgId"));
                    sub.setOrgName(as(row, "ORG_NAME", "orgName"));
                    sub.setParentOrgId(as(row, "PARENT_ORG_ID", "parentOrgId"));
                    sub.setOrgTypeCd(as(row, "ORG_TYPE_CD", "orgTypeCd"));
                    sub.setActiveYn(as(row, "ACTIVE_YN", "activeYn"));
                    sub.setRemark(as(row, "REMARK", "remark"));
                    sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                    sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                    sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                    sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                    out.addifina1500S0DTOSub0(sub);
                }
            }
        } else {
            total = dao.ifina1500S0_person_count(param);
            rows = dao.ifina1500S0_person(param);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    ifina1500S0DTOSub0 sub = new ifina1500S0DTOSub0();
                    sub.setEntityType("PERSON");
                    sub.setPersonId(as(row, "PERSON_ID", "personId"));
                    sub.setPersonName(as(row, "PERSON_NAME", "personName"));
                    sub.setOrgId(as(row, "ORG_ID", "orgId"));
                    sub.setOrgName(as(row, "ORG_NAME", "orgName"));
                    sub.setEmail(as(row, "EMAIL", "email"));
                    sub.setRoleCd(as(row, "ROLE_CD", "roleCd"));
                    sub.setActiveYn(as(row, "ACTIVE_YN", "activeYn"));
                    sub.setRemark(as(row, "REMARK", "remark"));
                    sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                    sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                    sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                    sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                    out.addifina1500S0DTOSub0(sub);
                }
            }
        }
        out.setSize(out.sizeifina1500S0DTOSub0());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina1500C0DTOout ifina1500C0(ifina1500C0DTOin input) throws Exception {
        ifina1500C0DTOout out = new ifina1500C0DTOout();
        if (authGuard.denyIfHard(out, "ifina1500C0")) return out;
        String entityType = blank(input == null ? null : input.getEntityType(), "PERSON").toUpperCase(Locale.ROOT);
        if ("ORG".equals(entityType)) {
            String orgId = trim(input.getOrgId());
            String orgName = trim(input.getOrgName());
            if (orgId == null || orgName == null) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: orgId, orgName");
                return out;
            }
            if (dao.ifina1500S0_org_exists(Map.of("orgId", orgId)) > 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0002");
                out.setRSLT_MSG("DUPLICATE_ORG_ID");
                return out;
            }
            String parent = trim(input.getParentOrgId());
            if (parent != null && dao.ifina1500S0_org_exists(Map.of("orgId", parent)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0004");
                out.setRSLT_MSG("상위조직 없음: " + parent);
                return out;
            }
            Map<String, Object> p = new HashMap<>();
            p.put("orgId", orgId);
            p.put("orgName", orgName);
            p.put("parentOrgId", empty(input.getParentOrgId()));
            p.put("orgTypeCd", blank(input.getOrgTypeCd(), "OPS"));
            p.put("activeYn", blank(input.getActiveYn(), "Y"));
            p.put("remark", empty(input.getRemark()));
            p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
            p.put("regDtm", now());
            p.put("chgUserId", null);
            p.put("chgDtm", null);
            out.setPROC_CNT(dao.ifina1500C0_org(p));
        } else {
            String personId = trim(input.getPersonId());
            String personName = trim(input.getPersonName());
            if (personId == null || personName == null) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: personId, personName");
                return out;
            }
            if (dao.ifina1500S0_person_exists(Map.of("personId", personId)) > 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0002");
                out.setRSLT_MSG("DUPLICATE_PERSON_ID");
                return out;
            }
            String orgId = trim(input.getOrgId());
            if (orgId != null && dao.ifina1500S0_org_exists(Map.of("orgId", orgId)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0004");
                out.setRSLT_MSG("조직 없음: " + orgId);
                return out;
            }
            Map<String, Object> p = new HashMap<>();
            p.put("personId", personId);
            p.put("personName", personName);
            p.put("orgId", empty(input.getOrgId()));
            p.put("email", empty(input.getEmail()));
            p.put("roleCd", blank(input.getRoleCd(), "OPS").toUpperCase(Locale.ROOT));
            p.put("activeYn", blank(input.getActiveYn(), "Y"));
            p.put("remark", empty(input.getRemark()));
            p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
            p.put("regDtm", now());
            p.put("chgUserId", null);
            p.put("chgDtm", null);
            out.setPROC_CNT(dao.ifina1500C0_person(p));
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina1500U0DTOout ifina1500U0(ifina1500U0DTOin input) throws Exception {
        ifina1500U0DTOout out = new ifina1500U0DTOout();
        if (authGuard.denyIfHard(out, "ifina1500U0")) return out;
        String entityType = blank(input == null ? null : input.getEntityType(), "PERSON").toUpperCase(Locale.ROOT);
        if ("ORG".equals(entityType)) {
            String orgId = trim(input.getOrgId());
            String orgName = trim(input.getOrgName());
            if (orgId == null || orgName == null) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: orgId, orgName");
                return out;
            }
            if (dao.ifina1500S0_org_exists(Map.of("orgId", orgId)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0003");
                out.setRSLT_MSG("NOT_FOUND");
                return out;
            }
            String parent = trim(input.getParentOrgId());
            if (parent != null && !parent.equals(orgId)
                    && dao.ifina1500S0_org_exists(Map.of("orgId", parent)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0004");
                out.setRSLT_MSG("상위조직 없음: " + parent);
                return out;
            }
            Map<String, Object> p = new HashMap<>();
            p.put("orgId", orgId);
            p.put("orgName", orgName);
            p.put("parentOrgId", empty(input.getParentOrgId()));
            p.put("orgTypeCd", empty(input.getOrgTypeCd()));
            p.put("activeYn", blank(input.getActiveYn(), "Y"));
            p.put("remark", empty(input.getRemark()));
            p.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
            p.put("chgDtm", now());
            out.setPROC_CNT(dao.ifina1500U0_org(p));
        } else {
            String personId = trim(input.getPersonId());
            String personName = trim(input.getPersonName());
            if (personId == null || personName == null) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: personId, personName");
                return out;
            }
            if (dao.ifina1500S0_person_exists(Map.of("personId", personId)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0003");
                out.setRSLT_MSG("NOT_FOUND");
                return out;
            }
            String orgId = trim(input.getOrgId());
            if (orgId != null && dao.ifina1500S0_org_exists(Map.of("orgId", orgId)) <= 0) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0004");
                out.setRSLT_MSG("조직 없음: " + orgId);
                return out;
            }
            Map<String, Object> p = new HashMap<>();
            p.put("personId", personId);
            p.put("personName", personName);
            p.put("orgId", empty(input.getOrgId()));
            p.put("email", empty(input.getEmail()));
            p.put("roleCd", blank(input.getRoleCd(), "OPS").toUpperCase(Locale.ROOT));
            p.put("activeYn", blank(input.getActiveYn(), "Y"));
            p.put("remark", empty(input.getRemark()));
            p.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
            p.put("chgDtm", now());
            out.setPROC_CNT(dao.ifina1500U0_person(p));
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    /**
     * OPEN-02 IdP 역할 동기화. entries: personId, idpRole|roleCd, (optional) personName, email, orgId.
     */
    public ifina1500E0DTOout ifina1500E0(ifina1500E0DTOin input) throws Exception {
        ifina1500E0DTOout out = new ifina1500E0DTOout();
        if (authGuard.denyIfHard(out, "ifina1500E0")) {
            return out;
        }
        if (!idpAuth.isEnabled()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("IdP sync disabled (infra.auth.idp.enabled=false)");
            return out;
        }
        List<Map<String, Object>> entries = input == null ? null : input.getEntries();
        if (entries == null || entries.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: entries[]");
            return out;
        }
        boolean dryRun = "Y".equalsIgnoreCase(blank(input.getDryRunYn(), "N"));
        boolean createMissing = input.getCreateMissingYn() != null && !input.getCreateMissingYn().isBlank()
                ? "Y".equalsIgnoreCase(input.getCreateMissingYn().trim())
                : idpAuth.isCreateMissing();

        int synced = 0;
        int created = 0;
        int skipped = 0;
        int errors = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        String now = now();

        for (Map<String, Object> entry : entries) {
            Map<String, Object> detail = new LinkedHashMap<>();
            String personId = entryStr(entry, "personId", "PERSON_ID");
            String idpRole = entryStr(entry, "idpRole", "IDP_ROLE");
            if (idpRole == null) {
                idpRole = entryStr(entry, "roleCd", "ROLE_CD");
            }
            detail.put("personId", personId);
            detail.put("idpRole", idpRole);

            if (personId == null) {
                errors++;
                detail.put("result", "ERROR");
                detail.put("message", "REQUIRED: personId");
                details.add(detail);
                continue;
            }
            String roleCd = idpAuth.resolveRaciRole(idpRole);
            if (roleCd == null) {
                errors++;
                detail.put("result", "ERROR");
                detail.put("message", "UNMAPPED_IDP_ROLE: " + idpRole);
                details.add(detail);
                continue;
            }
            detail.put("roleCd", roleCd);

            boolean exists = dao.ifina1500S0_person_exists(Map.of("personId", personId)) > 0;
            String beforeRole = exists ? dao.ifina1500S0_roleByPersonId(Map.of("personId", personId)) : null;
            detail.put("beforeRoleCd", beforeRole);

            if (exists && roleCd.equalsIgnoreCase(beforeRole == null ? "" : beforeRole.trim())) {
                skipped++;
                detail.put("result", dryRun ? "DRY_SKIP" : "SKIP");
                detail.put("message", "unchanged");
                details.add(detail);
                continue;
            }

            if (!exists && !createMissing) {
                errors++;
                detail.put("result", "ERROR");
                detail.put("message", "PERSON_NOT_FOUND");
                details.add(detail);
                continue;
            }

            if (dryRun) {
                if (!exists) {
                    created++;
                    detail.put("result", "DRY_CREATE");
                } else {
                    synced++;
                    detail.put("result", "DRY_OK");
                }
                details.add(detail);
                continue;
            }

            if (!exists) {
                String orgId = blank(entryStr(entry, "orgId", "ORG_ID"), idpAuth.getDefaultOrgId());
                if (dao.ifina1500S0_org_exists(Map.of("orgId", orgId)) <= 0) {
                    errors++;
                    detail.put("result", "ERROR");
                    detail.put("message", "조직 없음: " + orgId);
                    details.add(detail);
                    continue;
                }
                Map<String, Object> p = new HashMap<>();
                p.put("personId", personId);
                p.put("personName", blank(entryStr(entry, "personName", "PERSON_NAME"), personId));
                p.put("orgId", orgId);
                p.put("email", empty(entryStr(entry, "email", "EMAIL")));
                p.put("roleCd", roleCd);
                p.put("activeYn", "Y");
                p.put("remark", "IdP sync create");
                p.put("regUserId", "IDP");
                p.put("regDtm", now);
                p.put("chgUserId", null);
                p.put("chgDtm", null);
                dao.ifina1500C0_person(p);
                changeLogWriter.write("PERSON", personId, "IDP_CREATE", null, p, "ifina1500E0");
                created++;
                detail.put("result", "CREATED");
            } else {
                Map<String, Object> p = new HashMap<>();
                p.put("personId", personId);
                p.put("roleCd", roleCd);
                p.put("remark", "IdP sync " + (idpRole == null ? roleCd : idpRole));
                p.put("chgUserId", "IDP");
                p.put("chgDtm", now);
                dao.ifina1500E0_role(p);
                changeLogWriter.write(
                        "PERSON",
                        personId,
                        "IDP_SYNC",
                        Map.of("roleCd", beforeRole == null ? "" : beforeRole),
                        Map.of("roleCd", roleCd, "idpRole", idpRole == null ? "" : idpRole),
                        "ifina1500E0");
                synced++;
                detail.put("result", "SYNCED");
            }
            details.add(detail);
        }

        out.setSyncedCount(synced);
        out.setCreatedCount(created);
        out.setSkippedCount(skipped);
        out.setErrorCount(errors);
        out.setDetails(details);
        out.setPROC_CNT(synced + created);
        out.setRSLT_CD(errors > 0 && synced + created == 0 ? "0005" : "0000");
        out.setRSLT_MSG(dryRun ? "DRY_OK" : "OK");
        return out;
    }

    private static String entryStr(Map<String, Object> entry, String... keys) {
        if (entry == null) {
            return null;
        }
        for (String k : keys) {
            Object v = entry.get(k);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v).trim();
            }
        }
        for (Map.Entry<String, Object> e : entry.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            for (String k : keys) {
                if (e.getKey().equalsIgnoreCase(k) && !String.valueOf(e.getValue()).isBlank()) {
                    return String.valueOf(e.getValue()).trim();
                }
            }
        }
        return null;
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
}
