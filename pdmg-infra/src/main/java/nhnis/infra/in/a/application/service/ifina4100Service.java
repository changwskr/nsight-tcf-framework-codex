package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;

import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina1999DAO;
import nhnis.infra.in.a.persistence.dao.ifina4100DAO;

@Service
public class ifina4100Service {
    private final ifina4100DAO dao;
    private final ifina1999DAO assetDao;
    private final AuthGuard authGuard;

    public ifina4100Service(ifina4100DAO dao, ifina1999DAO assetDao, AuthGuard authGuard) {
        this.authGuard = authGuard; this.dao = dao; this.assetDao = assetDao; }

    public ifina4100S0DTOout ifina4100S0(ifina4100S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "mwId", input.getMwId());
            put(param, "assetId", input.getAssetId());
            put(param, "productName", input.getProductName());
            put(param, "statusCd", input.getStatusCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);
        int total = dao.ifina4100S0_S0_count(param);
        ifina4100S0DTOout out = new ifina4100S0DTOout();
        List<Map<String, Object>> rows = dao.ifina4100S0_S0(param);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                ifina4100S0DTOSub0 sub = new ifina4100S0DTOSub0();
                sub.setMwId(as(row, "MW_ID", "mwId"));
                sub.setAssetId(as(row, "ASSET_ID", "assetId"));
                sub.setProductName(as(row, "PRODUCT_NAME", "productName"));
                sub.setVersionNo(as(row, "VERSION_NO", "versionNo"));
                sub.setEolDate(as(row, "EOL_DATE", "eolDate"));
                sub.setStatusCd(as(row, "STATUS_CD", "statusCd"));
                sub.setRemark(as(row, "REMARK", "remark"));
                sub.setRegUserId(as(row, "REG_USER_ID", "regUserId"));
                sub.setRegDtm(as(row, "REG_DTM", "regDtm"));
                sub.setChgUserId(as(row, "CHG_USER_ID", "chgUserId"));
                sub.setChgDtm(as(row, "CHG_DTM", "chgDtm"));
                out.addifina4100S0DTOSub0(sub);
            }
        }
        out.setSize(out.sizeifina4100S0DTOSub0());
        out.setPageNo(pageNo); out.setPageSize(pageSize); out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina4100C0DTOout ifina4100C0(ifina4100C0DTOin input) throws Exception {
        ifina4100C0DTOout out = new ifina4100C0DTOout();
        if (authGuard.denyIfHard(out, "ifina4100C0")) return out;
        String mwId = trim(input == null ? null : input.getMwId());
        String assetId = trim(input == null ? null : input.getAssetId());
        String product = trim(input == null ? null : input.getProductName());
        if (mwId == null || assetId == null || product == null) {
            out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG("REQUIRED: mwId, assetId, productName"); return out;
        }
        if (assetDao.ifina1999S0_S0_exists(Map.of("serverId", assetId)) <= 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0004"); out.setRSLT_MSG("자산 없음: " + assetId); return out;
        }
        if (dao.ifina4100S0_S0_exists(Map.of("mwId", mwId)) > 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0002"); out.setRSLT_MSG("DUPLICATE_MW_ID"); return out;
        }
        Map<String, Object> param = base(input, mwId, true);
        out.setPROC_CNT(dao.ifina4100C0_C0(param));
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK"); return out;
    }

    public ifina4100U0DTOout ifina4100U0(ifina4100U0DTOin input) throws Exception {
        ifina4100U0DTOout out = new ifina4100U0DTOout();
        if (authGuard.denyIfHard(out, "ifina4100U0")) return out;
        String mwId = trim(input == null ? null : input.getMwId());
        String assetId = trim(input == null ? null : input.getAssetId());
        String product = trim(input == null ? null : input.getProductName());
        if (mwId == null || assetId == null || product == null) {
            out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG("REQUIRED: mwId, assetId, productName"); return out;
        }
        if (dao.ifina4100S0_S0_exists(Map.of("mwId", mwId)) <= 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0003"); out.setRSLT_MSG("NOT_FOUND"); return out;
        }
        if (assetDao.ifina1999S0_S0_exists(Map.of("serverId", assetId)) <= 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0004"); out.setRSLT_MSG("자산 없음: " + assetId); return out;
        }
        Map<String, Object> param = base(input, mwId, false);
        out.setPROC_CNT(dao.ifina4100U0_U0(param));
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK"); return out;
    }

    public ifina4100D0DTOout ifina4100D0(ifina4100D0DTOin input) throws Exception {
        ifina4100D0DTOout out = new ifina4100D0DTOout();
        if (authGuard.denyIfHard(out, "ifina4100D0")) return out;
        if (input == null || input.getMwIdList() == null || input.getMwIdList().isEmpty()) {
            out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG("NO_DATA"); return out;
        }
        List<String> ids = input.getMwIdList().stream().filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().toList();
        if (ids.isEmpty()) { out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG("NO_DATA"); return out; }
        out.setPROC_CNT(dao.ifina4100D0_D0(Map.of("mwIdList", ids)));
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK"); return out;
    }

    private Map<String, Object> base(Object input, String mwId, boolean create) {
        Map<String, Object> p = new HashMap<>();
        p.put("mwId", mwId);
        if (input instanceof ifina4100C0DTOin in) {
            p.put("assetId", empty(in.getAssetId()));
            p.put("productName", empty(in.getProductName()));
            p.put("versionNo", empty(in.getVersionNo()));
            p.put("eolDate", empty(in.getEolDate()));
            p.put("statusCd", blank(in.getStatusCd(), "DISCOVERED"));
            p.put("remark", empty(in.getRemark()));
            p.put("regUserId", blank(in.getRegUserId(), "LOCAL"));
            p.put("regDtm", now());
            p.put("chgUserId", null); p.put("chgDtm", null);
        } else if (input instanceof ifina4100U0DTOin in) {
            p.put("assetId", empty(in.getAssetId()));
            p.put("productName", empty(in.getProductName()));
            p.put("versionNo", empty(in.getVersionNo()));
            p.put("eolDate", empty(in.getEolDate()));
            p.put("statusCd", empty(in.getStatusCd()));
            p.put("remark", empty(in.getRemark()));
            p.put("chgUserId", blank(in.getChgUserId(), "LOCAL"));
            p.put("chgDtm", now());
        }
        return p;
    }

    private static int pageNo(Integer v){return v==null||v<=0?1:v;}
    private static int pageSize(Integer v){int s=v==null||v<=0?10:v;return Math.min(s,100);}
    private static String now(){return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());}
    private static String trim(String v){if(v==null)return null;String t=v.trim();return t.isEmpty()?null:t;}
    private static String empty(String v){return v==null?"":v.trim();}
    private static String blank(String v,String d){String t=trim(v);return t!=null?t:d;}
    private static void put(Map<String,Object> m,String k,String v){if(v!=null&&!v.isBlank())m.put(k,v.trim());}
    private static String as(Map<String,Object> row,String u,String c){
        if(row==null)return null; Object v=row.get(u); if(v==null)v=row.get(c);
        if(v==null){for(Map.Entry<String,Object> e:row.entrySet()){if(e.getKey()!=null&&(e.getKey().equalsIgnoreCase(u)||e.getKey().equalsIgnoreCase(c))){v=e.getValue();break;}}}
        return v==null?null:String.valueOf(v);
    }
}
