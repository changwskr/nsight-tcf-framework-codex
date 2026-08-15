package nhnis.infra.in.a.application.service;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina7200DAO;

@Service
public class ifina7200Service {
    private final ifina7200DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina7200Service(ifina7200DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina7200S0DTOout ifina7200S0(ifina7200S0DTOin input) throws Exception {
        ifina7200S0DTOout out = new ifina7200S0DTOout();
        String licenseId = trim(input == null ? null : input.getLicenseId());
        if (licenseId == null) {
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: licenseId");
            return out;
        }
        Map<String, Object> lic = dao.ifina7200S0_license(Map.of("licenseId", licenseId));
        if (lic == null || lic.isEmpty()) {
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        BigDecimal contractQty = toBd(val(lic, "QTY", "qty"));
        if (contractQty == null) contractQty = BigDecimal.ZERO;

        List<Map<String, Object>> allocations = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        List<Map<String, Object>> raw = dao.ifina7200S0_alloc(Map.of("licenseId", licenseId));
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("assetId", as(row, "ASSET_ID", "assetId"));
                m.put("licenseId", as(row, "LICENSE_ID", "licenseId"));
                BigDecimal qty = toBd(val(row, "ALLOCATED_QTY", "allocatedQty"));
                if (qty == null) qty = BigDecimal.ZERO;
                m.put("allocatedQty", qty);
                allocations.add(m);
                sum = sum.add(qty);
            }
        }

        out.setLicenseId(as(lic, "LICENSE_ID", "licenseId"));
        out.setProductName(as(lic, "PRODUCT_NAME", "productName"));
        out.setVendorName(as(lic, "VENDOR_NAME", "vendorName"));
        out.setLicenseModelCd(as(lic, "LICENSE_MODEL_CD", "licenseModelCd"));
        out.setContractQty(contractQty);
        out.setAllocatedSum(sum);
        out.setRemainingQty(contractQty.subtract(sum));
        out.setAllocations(allocations);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina7200U0DTOout ifina7200U0(ifina7200U0DTOin input) throws Exception {
        ifina7200U0DTOout out = new ifina7200U0DTOout();
        if (authGuard.denyIfHard(out, "ifina7200U0")) return out;
        String licenseId = trim(input == null ? null : input.getLicenseId());
        if (licenseId == null) {
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: licenseId");
            return out;
        }
        Map<String, Object> lic = dao.ifina7200S0_license(Map.of("licenseId", licenseId));
        if (lic == null || lic.isEmpty()) {
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        BigDecimal contractQty = toBd(val(lic, "QTY", "qty"));
        if (contractQty == null) contractQty = BigDecimal.ZERO;

        List<Map<String, Object>> incoming = input.getAllocations() == null ? List.of() : input.getAllocations();
        Map<String, BigDecimal> merged = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : incoming) {
            if (row == null) continue;
            String assetId = trim(str(row.get("assetId")));
            if (assetId == null) assetId = trim(str(row.get("ASSET_ID")));
            if (assetId == null) {
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("REQUIRED: allocations[].assetId");
                return out;
            }
            BigDecimal qty = toBd(row.get("allocatedQty"));
            if (qty == null) qty = toBd(row.get("ALLOCATED_QTY"));
            if (qty == null || qty.compareTo(BigDecimal.ZERO) < 0) {
                out.setRSLT_CD("0001");
                out.setRSLT_MSG("INVALID: allocatedQty");
                return out;
            }
            if (qty.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal prev = merged.getOrDefault(assetId, BigDecimal.ZERO);
            BigDecimal next = prev.add(qty);
            merged.put(assetId, next);
            sum = sum.add(qty);
        }

        if (sum.compareTo(contractQty) > 0) {
            out.setRSLT_CD("0004");
            out.setRSLT_MSG("[HARD] 할당합계(" + sum + ") > 계약Qty(" + contractQty + ")");
            return out;
        }

        List<Map<String, Object>> before = dao.ifina7200S0_alloc(Map.of("licenseId", licenseId));
        dao.ifina7200U0_deleteAll(Map.of("licenseId", licenseId));
        for (Map.Entry<String, BigDecimal> e : merged.entrySet()) {
            Map<String, Object> p = new HashMap<>();
            p.put("assetId", e.getKey());
            p.put("licenseId", licenseId);
            p.put("allocatedQty", e.getValue());
            dao.ifina7200U0_upsert(p);
        }
        changeLogWriter.write("LICENSE_ALLOC", licenseId, "UPDATE",
                Map.of("allocations", before == null ? List.of() : before),
                Map.of("allocations", merged, "allocatedSum", sum),
                "ifina7200U0");
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
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
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
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
