package nhnis.infra.in.a.application.service;

import java.util.*;
import org.springframework.stereotype.Service;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.dto.*;
import nhnis.infra.in.a.persistence.dao.ifina1300DAO;

@Service
public class ifina1300Service {
    private final ifina1300DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;
    public ifina1300Service(ifina1300DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.dao = dao; this.changeLogWriter = changeLogWriter; this.authGuard = authGuard;
    }

    public ifina1300S0DTOout ifina1300S0(ifina1300S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "checklistId", input.getChecklistId());
            put(param, "categoryKo", input.getCategoryKo());
            put(param, "activeYn", input.getActiveYn());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina1300S0_S0(param);
        if (raw != null) for (Map<String, Object> row : raw) rows.add(mapRow(row));
        int total = dao.ifina1300S0_S0_count(param);
        ifina1300S0DTOout out = new ifina1300S0DTOout();
        out.setRows(rows); out.setSize(rows.size()); out.setPageNo(pageNo); out.setPageSize(pageSize);
        out.setTotalCount(total); out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    public ifina1300C0DTOout ifina1300C0(ifina1300C0DTOin input) throws Exception {
        ifina1300C0DTOout out = new ifina1300C0DTOout();
        if (authGuard.denyIfHard(out, "ifina1300C0")) return out;
        String err = validate(input);
        if (err != null) { out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG(err); return out; }
        String id = trim(input.getChecklistId());
        if (dao.ifina1300S0_exists(Map.of("checklistId", id)) > 0) {
            out.setPROC_CNT(0); out.setRSLT_CD("0002"); out.setRSLT_MSG("DUPLICATE_CHECKLIST_ID"); return out;
        }
        Map<String, Object> p = toParam(input);
        out.setPROC_CNT(dao.ifina1300C0_C0(p));
        changeLogWriter.write("CHECKLIST_ITEM", id, "CREATE", null, p, "ifina1300C0");
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    public ifina1300U0DTOout ifina1300U0(ifina1300U0DTOin input) throws Exception {
        ifina1300U0DTOout out = new ifina1300U0DTOout();
        if (authGuard.denyIfHard(out, "ifina1300U0")) return out;
        String err = validate(input);
        if (err != null) { out.setPROC_CNT(0); out.setRSLT_CD("0001"); out.setRSLT_MSG(err); return out; }
        String id = trim(input.getChecklistId());
        Map<String, Object> before = dao.ifina1300S0_S1(Map.of("checklistId", id));
        if (before == null || before.isEmpty()) {
            out.setPROC_CNT(0); out.setRSLT_CD("0003"); out.setRSLT_MSG("NOT_FOUND"); return out;
        }
        Map<String, Object> p = toParam(input);
        out.setPROC_CNT(dao.ifina1300U0_U0(p));
        changeLogWriter.write("CHECKLIST_ITEM", id, "UPDATE", before, p, "ifina1300U0");
        out.setRSLT_CD("0000"); out.setRSLT_MSG("OK");
        return out;
    }

    private static String validate(ifina1300C0DTOin input) {
        if (input == null || trim(input.getChecklistId()) == null || trim(input.getItemName()) == null) {
            return "REQUIRED: checklistId, itemName";
        }
        return null;
    }
    private static Map<String, Object> toParam(ifina1300C0DTOin input) {
        Map<String, Object> p = new HashMap<>();
        p.put("checklistId", trim(input.getChecklistId()));
        p.put("itemName", trim(input.getItemName()));
        p.put("categoryKo", empty(input.getCategoryKo()));
        p.put("severityCd", blank(input.getSeverityCd(), "P2"));
        p.put("activeYn", blank(input.getActiveYn(), "Y").toUpperCase(Locale.ROOT));
        p.put("sortNo", input.getSortNo() == null ? 99 : input.getSortNo());
        p.put("remark", empty(input.getRemark()));
        return p;
    }
    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("checklistId", as(row, "CHECKLIST_ID", "checklistId"));
        m.put("itemName", as(row, "ITEM_NAME", "itemName"));
        m.put("categoryKo", as(row, "CATEGORY_KO", "categoryKo"));
        m.put("severityCd", as(row, "SEVERITY_CD", "severityCd"));
        m.put("activeYn", as(row, "ACTIVE_YN", "activeYn"));
        Object sn = row.get("SORT_NO"); if (sn == null) sn = row.get("sortNo");
        m.put("sortNo", sn);
        m.put("remark", as(row, "REMARK", "remark"));
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
