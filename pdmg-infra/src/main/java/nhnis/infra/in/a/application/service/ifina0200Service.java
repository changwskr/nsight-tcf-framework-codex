package nhnis.infra.in.a.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.dto.ifina0200S0DTOin;
import nhnis.infra.in.a.dto.ifina0200S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina0200DAO;

@Service
public class ifina0200Service {
    private final ifina0200DAO dao;

    public ifina0200Service(ifina0200DAO dao) {
        this.dao = dao;
    }

    public ifina0200S0DTOout ifina0200S0(ifina0200S0DTOin input) throws Exception {
        String keyword = trim(input == null ? null : input.getKeyword());
        String severityFilter = trim(input == null ? null : input.getSeverityCd());
        String riskType = blank(input == null ? null : input.getRiskType(), "ALL").toUpperCase(Locale.ROOT);
        int maxDays = input != null && input.getMaxDaysLeft() != null ? input.getMaxDaysLeft() : 365;

        Map<String, Object> p = new HashMap<>();
        if (keyword != null) p.put("keyword", keyword);
        if (severityFilter != null) p.put("severityCd", severityFilter);

        List<Map<String, Object>> rows = new ArrayList<>();
        long checklistCnt = 0, eolCnt = 0, gateCnt = 0;

        if ("ALL".equals(riskType) || "CHECKLIST".equals(riskType)) {
            List<Map<String, Object>> open = dao.ifina0200S0_checklistOpen(p);
            if (open != null) {
                checklistCnt = open.size();
                for (Map<String, Object> row : open) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("riskType", "CHECKLIST");
                    m.put("severityCd", as(row, "SEVERITY_CD", "severityCd"));
                    m.put("targetTypeCd", as(row, "TARGET_TYPE", "targetType"));
                    m.put("targetId", as(row, "TARGET_ID", "targetId"));
                    m.put("title", as(row, "ITEM_NAME", "itemName"));
                    m.put("detail", as(row, "CHECKLIST_ID", "checklistId"));
                    m.put("gateId", "GATE1");
                    m.put("remark", as(row, "REMARK", "remark"));
                    rows.add(m);
                }
            }
        }
        if ("ALL".equals(riskType) || "EOL".equals(riskType)) {
            List<Map<String, Object>> eol = dao.ifina0200S0_eol(p);
            if (eol != null) {
                for (Map<String, Object> row : eol) {
                    String eolDate = as(row, "EOL_DATE", "eolDate");
                    long days = ifina4300Service.daysLeft(eolDate);
                    if (days > maxDays) {
                        continue;
                    }
                    eolCnt++;
                    Map<String, Object> m = new HashMap<>();
                    m.put("riskType", "EOL");
                    m.put("severityCd", eolSeverity(days));
                    m.put("targetTypeCd", as(row, "SOURCE_CD", "sourceCd"));
                    m.put("targetId", as(row, "OBJECT_ID", "objectId"));
                    m.put("title", as(row, "OBJECT_NAME", "objectName") + " EOL " + eolDate);
                    m.put("detail", "asset=" + as(row, "ASSET_ID", "assetId") + ", daysLeft=" + days);
                    m.put("gateId", "GATE1");
                    m.put("remark", days + "일 남음");
                    rows.add(m);
                }
            }
        }
        if ("ALL".equals(riskType) || "GATE".equals(riskType)) {
            List<Map<String, Object>> gates = dao.ifina0200S0_gateOpen(p);
            if (gates != null) {
                gateCnt = gates.size();
                for (Map<String, Object> row : gates) {
                    String resultCd = as(row, "RESULT_CD", "resultCd");
                    Map<String, Object> m = new HashMap<>();
                    m.put("riskType", "GATE");
                    m.put("severityCd", "FAIL".equalsIgnoreCase(resultCd) ? "P0" : "P1");
                    m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
                    m.put("targetId", as(row, "TARGET_ID", "targetId"));
                    m.put("title", as(row, "GATE_ID", "gateId") + " " + as(row, "GATE_NAME", "gateName"));
                    m.put("detail", resultCd);
                    m.put("gateId", as(row, "GATE_ID", "gateId"));
                    m.put("remark", as(row, "REMARK", "remark"));
                    rows.add(m);
                }
            }
        }

        if (severityFilter != null) {
            String sev = severityFilter;
            rows = rows.stream().filter(r -> sev.equalsIgnoreCase(String.valueOf(r.get("severityCd")))).toList();
            rows = new ArrayList<>(rows);
        }
        rows.sort(Comparator
                .comparing((Map<String, Object> r) -> sevRank(String.valueOf(r.get("severityCd"))))
                .thenComparing(r -> String.valueOf(r.get("riskType")))
                .thenComparing(r -> String.valueOf(r.get("targetId"))));

        ifina0200S0DTOout out = new ifina0200S0DTOout();
        out.setRows(rows);
        out.setSize(rows.size());
        out.setChecklistOpenCount(checklistCnt);
        out.setEolCount(eolCnt);
        out.setGateOpenCount(gateCnt);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static int sevRank(String s) {
        if ("P0".equalsIgnoreCase(s)) return 0;
        if ("P1".equalsIgnoreCase(s)) return 1;
        if ("P2".equalsIgnoreCase(s)) return 2;
        return 3;
    }

    private static String eolSeverity(long d) {
        if (d <= 90) return "P0";
        if (d <= 180) return "P1";
        return "P2";
    }

    private static String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String blank(String v, String d) {
        String t = trim(v);
        return t != null ? t : d;
    }

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
