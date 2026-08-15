package nhnis.infra.in.a.application.service;

import java.util.*;
import org.springframework.stereotype.Service;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina1400DAO;

@Service
public class ifina1400Service {
    private final ifina1400DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;
    public ifina1400Service(ifina1400DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.dao = dao; this.changeLogWriter = changeLogWriter; this.authGuard = authGuard;
    }

    public ifina1400S0DTOout ifina1400S0(ifina1400S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "gateId", input.getGateId());
            put(param, "activeYn", input.getActiveYn());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina1400S0_S0(param);
        if (raw != null) for (Map<String, Object> row : raw) rows.add(mapRow(row));
        int total = dao.ifina1400S0_S0_count(param);
        ifina1400S0DTOout out = new ifina1400S0DTOout();
        out.setRows(rows); out.setSize(rows.size()); out.setPageNo(pageNo); out.setPageSize(pageSize);
        out.setTotalCount(total); out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    public ifina1400C0DTOout ifina1400C0(ifina1400C0DTOin input) throws Exception {
        ifina1400C0DTOout out = new ifina1400C0DTOout();
        if (authGuard.denyIfHard(out, "ifina1400C0")) return out;
        String err = validate(input);
        if (err != null) { out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG(err); return out; }
        String id = trim(input.getGateId()).toUpperCase(Locale.ROOT);
        if (dao.ifina1400S0_exists(Map.of("gateId", id)) > 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0002"); out.setRSLT_MSG("DUPLICATE_GATE_ID"); return out;
        }
        Map<String, Object> p = toParam(input, id);
        out.setPROC_CNT(dao.ifina1400C0_C0(p));
        changeLogWriter.write("GATE_DEF", id, "CREATE", null, p, "ifina1400C0");
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    public ifina1400U0DTOout ifina1400U0(ifina1400U0DTOin input) throws Exception {
        ifina1400U0DTOout out = new ifina1400U0DTOout();
        if (authGuard.denyIfHard(out, "ifina1400U0")) return out;
        String err = validate(input);
        if (err != null) { out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG(err); return out; }
        String id = trim(input.getGateId()).toUpperCase(Locale.ROOT);
        Map<String, Object> before = dao.ifina1400S0_S1(Map.of("gateId", id));
        if (before == null || before.isEmpty()) {
            out.setPROC_CNT(0); out.setRSLT_CD("0003"); out.setRSLT_MSG("NOT_FOUND"); return out;
        }
        Map<String, Object> p = toParam(input, id);
        out.setPROC_CNT(dao.ifina1400U0_U0(p));
        changeLogWriter.write("GATE_DEF", id, "UPDATE", before, p, "ifina1400U0");
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    private static String validate(ifina1400C0DTOin input) {
        if (input == null || trim(input.getGateId()) == null || trim(input.getNameKo()) == null) {
            return "REQUIRED: gateId, nameKo";
        }
        return null;
    }
    private static Map<String, Object> toParam(ifina1400C0DTOin input, String id) {
        Map<String, Object> p = new HashMap<>();
        p.put("gateId", id);
        p.put("nameKo", trim(input.getNameKo()));
        p.put("description", empty(input.getDescription()));
        p.put("sortNo", input.getSortNo() == null ? 99 : input.getSortNo());
        p.put("activeYn", blank(input.getActiveYn(), "Y").toUpperCase(Locale.ROOT));
        return p;
    }
    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gateId", as(row, "GATE_ID", "gateId"));
        m.put("nameKo", as(row, "NAME_KO", "nameKo"));
        m.put("description", as(row, "DESCRIPTION", "description"));
        Object sn = row.get("SORT_NO"); if (sn == null) sn = row.get("sortNo");
        m.put("sortNo", sn);
        m.put("activeYn", as(row, "ACTIVE_YN", "activeYn"));
        return m;
    }
    private static int pageNo(Integer v){return v==null||v<=0?1:v;}
    private static int pageSize(Integer v){int s=v==null||v<=0?50:v; return Math.min(s,200);}
    private static String trim(String v){if(v==null)return null; String t=v.trim(); return t.isEmpty()?null:t;}
    private static String empty(String v){return v==null?"":v.trim();}
    private static String blank(String v,String d){String t=trim(v); return t!=null?t:d;}
    private static void put(Map<String,Object> m,String k,String v){if(v!=null&&!v.isBlank())m.put(k,v.trim());}
    private static String as(Map<String,Object> row,String u,String c){
        Object v=row.get(u); if(v==null)v=row.get(c);
        if(v==null) for(Map.Entry<String,Object> e:row.entrySet()) if(e.getKey()!=null&&(e.getKey().equalsIgnoreCase(u)||e.getKey().equalsIgnoreCase(c))){v=e.getValue();break;}
        return v==null?null:String.valueOf(v);
    }
}
