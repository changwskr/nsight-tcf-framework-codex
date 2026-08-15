package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.V0ResponseMapper;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina6300DAO;

@Service
public class ifina6300Service {
    private final ifina6300DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina6300Service(ifina6300DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina6300S0DTOout ifina6300S0(ifina6300S0DTOin input) throws Exception {
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = blank(input == null ? null : input.getTargetId(), "SG-WAS-A");
        Map<String, Object> row = dao.ifina6300S0_S1(Map.of("targetTypeCd", targetType, "targetId", targetId));
        ifina6300S0DTOout out = new ifina6300S0DTOout();
        out.setTargetTypeCd(targetType);
        out.setTargetId(targetId);
        List<String> warnings = new ArrayList<>();
        if (row == null || row.isEmpty()) {
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            warnings.add("[RL-SC-001] security_profile 없음");
            out.setWarnings(warnings);
            return out;
        }
        fill(out, row);
        warnings.addAll(evaluateSoft(out));
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifinaV0DTOout ifina6300V0(ifina6300S0DTOin input) throws Exception {
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = blank(input == null ? null : input.getTargetId(), "SG-WAS-A");
        Map<String, Object> row = dao.ifina6300S0_S1(Map.of("targetTypeCd", targetType, "targetId", targetId));
        return V0ResponseMapper.from(evaluate(row), targetType + ":" + targetId);
    }

    public ifina6300U0DTOout ifina6300U0(ifina6300U0DTOin input) throws Exception {
        ifina6300U0DTOout out = new ifina6300U0DTOout();
        if (authGuard.denyIfHard(out, "ifina6300U0")) return out;
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String targetId = trim(input == null ? null : input.getTargetId());
        if (targetId == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: targetId");
            return out;
        }
        Map<String, Object> before = dao.ifina6300S0_S1(Map.of("targetTypeCd", targetType, "targetId", targetId));
        Map<String, Object> p = new HashMap<>();
        p.put("targetTypeCd", targetType);
        p.put("targetId", targetId);
        p.put("securityGradeCd", blank(input.getSecurityGradeCd(), "GENERAL"));
        p.put("personalInfoYn", yn(input.getPersonalInfoYn()));
        p.put("creditInfoYn", yn(input.getCreditInfoYn()));
        p.put("financialTxnYn", yn(input.getFinancialTxnYn()));
        p.put("adminInfoYn", yn(input.getAdminInfoYn()));
        p.put("externalConnYn", yn(input.getExternalConnYn()));
        p.put("internetConnYn", yn(input.getInternetConnYn()));
        p.put("encryptionYn", yn(input.getEncryptionYn()));
        p.put("kmsHsmYn", yn(input.getKmsHsmYn()));
        p.put("pamYn", yn(input.getPamYn()));
        p.put("edrYn", yn(input.getEdrYn()));
        p.put("auditLogYn", yn(input.getAuditLogYn()));
        p.put("authMethodCd", empty(input.getAuthMethodCd()));
        p.put("networkZoneCd", empty(input.getNetworkZoneCd()));
        p.put("remark", empty(input.getRemark()));

        List<String> warnings = evaluateSoftMap(p);
        int cnt;
        if (dao.ifina6300S0_exists(p) > 0) {
            p.put("chgUserId", "LOCAL");
            p.put("chgDtm", now());
            cnt = dao.ifina6300U0_update(p);
        } else {
            p.put("profileId", blank(input.getProfileId(), "SEC-" + targetId));
            p.put("regUserId", "LOCAL");
            p.put("regDtm", now());
            cnt = dao.ifina6300U0_insert(p);
        }
        changeLogWriter.write("SECURITY", targetType + ":" + targetId, "UPSERT", before, p, "ifina6300U0");
        out.setPROC_CNT(cnt);
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    /** V0 / Gate5: 프로파일 없음=HARD RL-SC-001, 나머지는 Soft. */
    public static ValidationResult evaluate(Map<String, Object> row) {
        ValidationResult r = new ValidationResult();
        if (row == null || row.isEmpty()) {
            r.add(RuleViolation.hard("RL-SC-001", "0001", "security_profile 없음"));
            return r;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("personalInfoYn", as(row, "PERSONAL_INFO_YN", "personalInfoYn"));
        p.put("creditInfoYn", as(row, "CREDIT_INFO_YN", "creditInfoYn"));
        p.put("encryptionYn", as(row, "ENCRYPTION_YN", "encryptionYn"));
        p.put("pamYn", as(row, "PAM_YN", "pamYn"));
        p.put("adminInfoYn", as(row, "ADMIN_INFO_YN", "adminInfoYn"));
        p.put("externalConnYn", as(row, "EXTERNAL_CONN_YN", "externalConnYn"));
        p.put("auditLogYn", as(row, "AUDIT_LOG_YN", "auditLogYn"));
        p.put("networkZoneCd", as(row, "NETWORK_ZONE_CD", "networkZoneCd"));
        for (String w : evaluateSoftMap(p)) {
            String ruleId = w.startsWith("[RL-SC-003]") ? "RL-SC-003" : "RL-SC-002";
            String msg = w.replaceFirst("^\\[RL-SC-00[23]\\]\\s*", "");
            r.add(RuleViolation.soft(ruleId, msg));
        }
        return r;
    }

    static List<String> evaluateSoft(ifina6300S0DTOout o) {
        Map<String, Object> p = new HashMap<>();
        p.put("personalInfoYn", o.getPersonalInfoYn());
        p.put("creditInfoYn", o.getCreditInfoYn());
        p.put("encryptionYn", o.getEncryptionYn());
        p.put("pamYn", o.getPamYn());
        p.put("adminInfoYn", o.getAdminInfoYn());
        p.put("externalConnYn", o.getExternalConnYn());
        p.put("auditLogYn", o.getAuditLogYn());
        p.put("networkZoneCd", o.getNetworkZoneCd());
        return evaluateSoftMap(p);
    }

    public static List<String> evaluateSoftMap(Map<String, Object> p) {
        List<String> w = new ArrayList<>();
        if (isY(p.get("personalInfoYn")) && !isY(p.get("encryptionYn"))) {
            w.add("[RL-SC-002] 개인정보=Y + Encryption=N");
        }
        if (isY(p.get("personalInfoYn")) && !isY(p.get("pamYn"))) {
            w.add("[RL-SC-002] 개인정보=Y + PAM=N");
        }
        if (isY(p.get("adminInfoYn")) && !isY(p.get("pamYn"))) {
            w.add("[RL-SC-002] 관리자정보=Y + PAM=N");
        }
        if (isY(p.get("externalConnYn")) && !isY(p.get("auditLogYn"))) {
            w.add("[RL-SC-002] 외부연계=Y + Audit=N");
        }
        if (isY(p.get("creditInfoYn")) && (!isY(p.get("encryptionYn")) || isBlank(str(p.get("networkZoneCd"))))) {
            w.add("[RL-SC-003] 신용정보=Y + 구역/암호화 미흡");
        }
        return w;
    }

    private static void fill(ifina6300S0DTOout out, Map<String, Object> row) {
        out.setProfileId(as(row, "PROFILE_ID", "profileId"));
        out.setSecurityGradeCd(as(row, "SECURITY_GRADE_CD", "securityGradeCd"));
        out.setPersonalInfoYn(as(row, "PERSONAL_INFO_YN", "personalInfoYn"));
        out.setCreditInfoYn(as(row, "CREDIT_INFO_YN", "creditInfoYn"));
        out.setFinancialTxnYn(as(row, "FINANCIAL_TXN_YN", "financialTxnYn"));
        out.setAdminInfoYn(as(row, "ADMIN_INFO_YN", "adminInfoYn"));
        out.setExternalConnYn(as(row, "EXTERNAL_CONN_YN", "externalConnYn"));
        out.setInternetConnYn(as(row, "INTERNET_CONN_YN", "internetConnYn"));
        out.setEncryptionYn(as(row, "ENCRYPTION_YN", "encryptionYn"));
        out.setKmsHsmYn(as(row, "KMS_HSM_YN", "kmsHsmYn"));
        out.setPamYn(as(row, "PAM_YN", "pamYn"));
        out.setEdrYn(as(row, "EDR_YN", "edrYn"));
        out.setAuditLogYn(as(row, "AUDIT_LOG_YN", "auditLogYn"));
        out.setAuthMethodCd(as(row, "AUTH_METHOD_CD", "authMethodCd"));
        out.setNetworkZoneCd(as(row, "NETWORK_ZONE_CD", "networkZoneCd"));
        out.setRemark(as(row, "REMARK", "remark"));
    }

    private static boolean isY(Object v) { return "Y".equalsIgnoreCase(str(v)); }
    private static String yn(String v) { return blank(v, "N").toUpperCase(Locale.ROOT); }
    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static boolean isBlank(String v) { return v == null || v.isBlank(); }
    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
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
