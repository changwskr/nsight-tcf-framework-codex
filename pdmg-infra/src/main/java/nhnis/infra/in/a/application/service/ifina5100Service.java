package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.dto.ifina5100C0DTOin;
import nhnis.infra.in.a.dto.ifina5100C0DTOout;
import nhnis.infra.in.a.dto.ifina5100D0DTOin;
import nhnis.infra.in.a.dto.ifina5100D0DTOout;
import nhnis.infra.in.a.dto.ifina5100S0DTOSub0;
import nhnis.infra.in.a.dto.ifina5100S0DTOin;
import nhnis.infra.in.a.dto.ifina5100S0DTOout;
import nhnis.infra.in.a.dto.ifina5100U0DTOin;
import nhnis.infra.in.a.dto.ifina5100U0DTOout;
import nhnis.infra.in.a.application.support.InfraIntegrityValidator;
import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.persistence.dao.ifina5100DAO;

@Service
public class ifina5100Service {
    private final ifina5100DAO dao;
    private final InfraIntegrityValidator validator;
    private final AuthGuard authGuard;

    public ifina5100Service(ifina5100DAO dao, InfraIntegrityValidator validator, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.validator = validator;
    }

    public ifina5100S0DTOout ifina5100S0(ifina5100S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "endpointId", input.getEndpointId());
            put(param, "assetId", input.getAssetId());
            put(param, "address", input.getAddress());
            put(param, "endpointTypeCd", input.getEndpointTypeCd());
            put(param, "primaryYn", input.getPrimaryYn());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);
        int total = dao.ifina5100S0_S0_count(param);
        ifina5100S0DTOout out = new ifina5100S0DTOout();
        List<Map<String, Object>> rows = dao.ifina5100S0_S0(param);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                ifina5100S0DTOSub0 sub = new ifina5100S0DTOSub0();
                sub.setEndpointId(as(row, "ENDPOINT_ID", "endpointId"));
                sub.setAssetId(as(row, "ASSET_ID", "assetId"));
                sub.setEndpointTypeCd(as(row, "ENDPOINT_TYPE_CD", "endpointTypeCd"));
                sub.setAddress(as(row, "ADDRESS", "address"));
                sub.setPortNo(as(row, "PORT_NO", "portNo"));
                sub.setProtocolCd(as(row, "PROTOCOL_CD", "protocolCd"));
                sub.setPrimaryYn(as(row, "PRIMARY_YN", "primaryYn"));
                sub.setRemark(as(row, "REMARK", "remark"));
                sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                out.addifina5100S0DTOSub0(sub);
            }
        }
        out.setSize(out.sizeifina5100S0DTOSub0());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina5100C0DTOout ifina5100C0(ifina5100C0DTOin input) throws Exception {
        ifina5100C0DTOout out = new ifina5100C0DTOout();
        if (authGuard.denyIfHard(out, "ifina5100C0")) return out;
        String endpointId = trim(input == null ? null : input.getEndpointId());
        String assetId = trim(input == null ? null : input.getAssetId());
        String address = trim(input == null ? null : input.getAddress());
        String portNo = empty(input == null ? null : input.getPortNo());
        ValidationResult vr = validator.validateNetworkEndpoint(endpointId, assetId, address, portNo, true);
        if (vr.hasHard()) {
            RuleViolation h = vr.firstHard().orElseThrow();
            out.setPROC_CNT(0);
            out.setRSLT_CD(h.getRsltCd());
            out.setRSLT_MSG(h.formatted());
            return out;
        }
        if (dao.ifina5100S0_S0_exists(Map.of("endpointId", endpointId)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_ENDPOINT_ID");
            return out;
        }
        String primaryYn = blank(input.getPrimaryYn(), "N").toUpperCase(Locale.ROOT);
        if ("Y".equals(primaryYn)) {
            dao.ifina5100U0_clearPrimary(Map.of("assetId", assetId));
        }
        Map<String, Object> p = new HashMap<>();
        p.put("endpointId", endpointId);
        p.put("assetId", assetId);
        p.put("endpointTypeCd", blank(input.getEndpointTypeCd(), "MGMT"));
        p.put("address", address);
        p.put("portNo", portNo);
        p.put("protocolCd", blank(input.getProtocolCd(), "TCP"));
        p.put("primaryYn", primaryYn);
        p.put("remark", empty(input.getRemark()));
        p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
        p.put("regDtm", now());
        p.put("chgUserId", null);
        p.put("chgDtm", null);
        out.setPROC_CNT(dao.ifina5100C0_C0(p));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina5100U0DTOout ifina5100U0(ifina5100U0DTOin input) throws Exception {
        ifina5100U0DTOout out = new ifina5100U0DTOout();
        if (authGuard.denyIfHard(out, "ifina5100U0")) return out;
        String endpointId = trim(input == null ? null : input.getEndpointId());
        String assetId = trim(input == null ? null : input.getAssetId());
        String address = trim(input == null ? null : input.getAddress());
        String portNo = empty(input == null ? null : input.getPortNo());
        if (dao.ifina5100S0_S0_exists(Map.of("endpointId", endpointId == null ? "" : endpointId)) <= 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0003");
            out.setRSLT_MSG("NOT_FOUND");
            return out;
        }
        ValidationResult vr = validator.validateNetworkEndpoint(endpointId, assetId, address, portNo, false);
        if (vr.hasHard()) {
            RuleViolation h = vr.firstHard().orElseThrow();
            out.setPROC_CNT(0);
            out.setRSLT_CD(h.getRsltCd());
            out.setRSLT_MSG(h.formatted());
            return out;
        }
        String primaryYn = blank(input.getPrimaryYn(), "N").toUpperCase(Locale.ROOT);
        if ("Y".equals(primaryYn)) {
            Map<String, Object> clear = new HashMap<>();
            clear.put("assetId", assetId);
            clear.put("endpointId", endpointId);
            dao.ifina5100U0_clearPrimary(clear);
        }
        Map<String, Object> p = new HashMap<>();
        p.put("endpointId", endpointId);
        p.put("assetId", assetId);
        p.put("endpointTypeCd", empty(input.getEndpointTypeCd()));
        p.put("address", address);
        p.put("portNo", portNo);
        p.put("protocolCd", empty(input.getProtocolCd()));
        p.put("primaryYn", primaryYn);
        p.put("remark", empty(input.getRemark()));
        p.put("chgUserId", blank(input.getChgUserId(), "LOCAL"));
        p.put("chgDtm", now());
        out.setPROC_CNT(dao.ifina5100U0_U0(p));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina5100D0DTOout ifina5100D0(ifina5100D0DTOin input) throws Exception {
        ifina5100D0DTOout out = new ifina5100D0DTOout();
        if (authGuard.denyIfHard(out, "ifina5100D0")) return out;
        if (input == null || input.getEndpointIdList() == null || input.getEndpointIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getEndpointIdList().stream()
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
        out.setPROC_CNT(dao.ifina5100D0_D0(Map.of("endpointIdList", ids)));
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
