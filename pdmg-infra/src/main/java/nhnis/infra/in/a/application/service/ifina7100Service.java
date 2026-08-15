package nhnis.infra.in.a.application.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina7100DAO;

@Service
public class ifina7100Service {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ifina7100DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina7100Service(ifina7100DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina7100S0DTOout ifina7100S0(ifina7100S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "vendorName", input.getVendorName());
            put(param, "licenseModelCd", input.getLicenseModelCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina7100S0_S0(param);
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Map<String, Object> m = mapRow(row);
                rows.add(m);
                String w = expireWarn(str(m.get("licenseId")), str(m.get("contractEndDt")));
                if (w != null) warnings.add(w);
            }
        }

        List<Map<String, Object>> allocs = new ArrayList<>();
        String detailId = trim(input == null ? null : input.getLicenseId());
        if (detailId != null) {
            List<Map<String, Object>> a = dao.ifina7100S0_alloc(Map.of("licenseId", detailId));
            if (a != null) {
                for (Map<String, Object> row : a) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("assetId", as(row, "ASSET_ID", "assetId"));
                    m.put("licenseId", as(row, "LICENSE_ID", "licenseId"));
                    m.put("allocatedQty", toBd(val(row, "ALLOCATED_QTY", "allocatedQty")));
                    allocs.add(m);
                }
            }
        }

        int total = dao.ifina7100S0_S0_count(param);
        ifina7100S0DTOout out = new ifina7100S0DTOout();
        out.setRows(rows);
        out.setAllocations(allocs);
        out.setWarnings(warnings);
        out.setSize(rows.size());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifina7100C0DTOout ifina7100C0(ifina7100C0DTOin input) throws Exception {
        ifina7100C0DTOout out = new ifina7100C0DTOout();
        if (authGuard.denyIfHard(out, "ifina7100C0")) return out;
        String err = validate(input);
        if (err != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(err);
            return out;
        }
        String id = trim(input.getLicenseId());
        if (dao.ifina7100S0_exists(Map.of("licenseId", id)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_LICENSE_ID");
            return out;
        }
        Map<String, Object> p = toParam(input);
        p.put("regUserId", "LOCAL");
        p.put("regDtm", now());
        out.setPROC_CNT(dao.ifina7100C0_C0(p));
        List<String> warnings = soft(p);
        changeLogWriter.write("LICENSE", id, "CREATE", null, p, "ifina7100C0");
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifina7100U0DTOout ifina7100U0(ifina7100U0DTOin input) throws Exception {
        ifina7100U0DTOout out = new ifina7100U0DTOout();
        if (authGuard.denyIfHard(out, "ifina7100U0")) return out;
        String err = validate(input);
        if (err != null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG(err);
            return out;
        }
        String id = trim(input.getLicenseId());
        Map<String, Object> before = dao.ifina7100S0_S1(Map.of("licenseId", id));
        if (before == null || before.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        Map<String, Object> p = toParam(input);
        p.put("chgUserId", "LOCAL");
        p.put("chgDtm", now());
        out.setPROC_CNT(dao.ifina7100U0_U0(p));
        List<String> warnings = soft(p);
        changeLogWriter.write("LICENSE", id, "UPDATE", before, p, "ifina7100U0");
        out.setWarnings(warnings);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(warnings.isEmpty() ? "OK" : String.join("; ", warnings));
        return out;
    }

    public ifina7100D0DTOout ifina7100D0(ifina7100D0DTOin input) throws Exception {
        ifina7100D0DTOout out = new ifina7100D0DTOout();
        if (authGuard.denyIfHard(out, "ifina7100D0")) return out;
        if (input == null || input.getLicenseIdList() == null || input.getLicenseIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getLicenseIdList().stream()
                .filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
        if (ids.isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        out.setPROC_CNT(dao.ifina7100D0_D0(Map.of("licenseIdList", ids)));
        for (String id : ids) {
            changeLogWriter.write("LICENSE", id, "DELETE", Map.of("licenseId", id), null, "ifina7100D0");
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static String validate(ifina7100C0DTOin input) {
        if (input == null || trim(input.getLicenseId()) == null || trim(input.getProductName()) == null) {
            return "REQUIRED: licenseId, productName";
        }
        return null;
    }

    private static List<String> soft(Map<String, Object> p) {
        List<String> w = new ArrayList<>();
        String warn = expireWarn(str(p.get("licenseId")), str(p.get("contractEndDt")));
        if (warn != null) w.add(warn);
        return w;
    }

    static String expireWarn(String licenseId, String endDt) {
        if (endDt == null || endDt.isBlank()) return null;
        try {
            LocalDate end = LocalDate.parse(endDt.trim(), ISO);
            long days = ChronoUnit.DAYS.between(LocalDate.of(2026, 8, 15), end);
            if (days <= 180) {
                return "[RL-LC-001] " + licenseId + " 계약만료 " + days + "일 남음";
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static Map<String, Object> toParam(ifina7100C0DTOin input) {
        Map<String, Object> p = new HashMap<>();
        p.put("licenseId", trim(input.getLicenseId()));
        p.put("productName", trim(input.getProductName()));
        p.put("vendorName", empty(input.getVendorName()));
        p.put("licenseModelCd", blank(input.getLicenseModelCd(), "CORE").toUpperCase(Locale.ROOT));
        p.put("qty", input.getQty());
        p.put("annualMaintAmt", input.getAnnualMaintAmt());
        p.put("currencyCd", blank(input.getCurrencyCd(), "KRW"));
        p.put("contractEndDt", empty(input.getContractEndDt()));
        p.put("mobilityYn", blank(input.getMobilityYn(), "N").toUpperCase(Locale.ROOT));
        p.put("remark", empty(input.getRemark()));
        return p;
    }

    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("licenseId", as(row, "LICENSE_ID", "licenseId"));
        m.put("productName", as(row, "PRODUCT_NAME", "productName"));
        m.put("vendorName", as(row, "VENDOR_NAME", "vendorName"));
        m.put("licenseModelCd", as(row, "LICENSE_MODEL_CD", "licenseModelCd"));
        m.put("qty", toBd(val(row, "QTY", "qty")));
        m.put("annualMaintAmt", toBd(val(row, "ANNUAL_MAINT_AMT", "annualMaintAmt")));
        m.put("currencyCd", as(row, "CURRENCY_CD", "currencyCd"));
        m.put("contractEndDt", as(row, "CONTRACT_END_DT", "contractEndDt"));
        m.put("mobilityYn", as(row, "MOBILITY_YN", "mobilityYn"));
        m.put("allocatedQty", toBd(val(row, "ALLOCATED_QTY", "allocatedQty")));
        m.put("remark", as(row, "REMARK", "remark"));
        return m;
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return null;
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception e) { return null; }
    }
    private static Object val(Map<String, Object> row, String u, String c) {
        Object v = row.get(u);
        if (v == null) v = row.get(c);
        return v;
    }
    private static int pageNo(Integer v) { return v == null || v <= 0 ? 1 : v; }
    private static int pageSize(Integer v) { int s = v == null || v <= 0 ? 50 : v; return Math.min(s, 200); }
    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static void put(Map<String, Object> m, String k, String v) { if (v != null && !v.isBlank()) m.put(k, v.trim()); }
    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
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
