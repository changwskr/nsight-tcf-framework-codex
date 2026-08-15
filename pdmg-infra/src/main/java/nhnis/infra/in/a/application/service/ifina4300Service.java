package nhnis.infra.in.a.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.dto.ifina4300S0DTOin;
import nhnis.infra.in.a.dto.ifina4300S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina4300DAO;

@Service
public class ifina4300Service {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ifina4300DAO dao;

    public ifina4300Service(ifina4300DAO dao) {
        this.dao = dao;
    }

    public ifina4300S0DTOout ifina4300S0(ifina4300S0DTOin input) throws Exception {
        int maxDays = input != null && input.getMaxDaysLeft() != null ? input.getMaxDaysLeft() : 365;
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "sourceCd", input.getSourceCd());
        }
        // fetch enough then filter by days in Java (H2 view date math 회피)
        param.put("offset", 0);
        param.put("pageSize", 500);
        List<Map<String, Object>> raw = dao.ifina4300S0_S0(param);
        List<Map<String, Object>> filtered = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                String eol = as(row, "EOL_DATE", "eolDate");
                long days = daysLeft(eol);
                if (days > maxDays) {
                    continue;
                }
                Map<String, Object> m = new HashMap<>();
                m.put("sourceCd", as(row, "SOURCE_CD", "sourceCd"));
                m.put("objectId", as(row, "OBJECT_ID", "objectId"));
                m.put("objectName", as(row, "OBJECT_NAME", "objectName"));
                m.put("assetId", as(row, "ASSET_ID", "assetId"));
                m.put("eolDate", eol);
                m.put("daysLeft", String.valueOf(days));
                m.put("severityCd", severity(days));
                filtered.add(m);
            }
        }
        filtered.sort(Comparator.comparingLong(r -> Long.parseLong(String.valueOf(r.get("daysLeft")))));

        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        int total = filtered.size();
        int from = Math.min((pageNo - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> page = filtered.subList(from, to);

        ifina4300S0DTOout out = new ifina4300S0DTOout();
        out.setRows(new ArrayList<>(page));
        out.setSize(page.size());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    static long daysLeft(String eolDate) {
        if (eolDate == null || eolDate.isBlank()) {
            return 9999;
        }
        try {
            return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(eolDate.trim(), ISO));
        } catch (Exception e) {
            return 9999;
        }
    }

    private static String severity(long d) {
        if (d <= 90) return "P0";
        if (d <= 180) return "P1";
        if (d <= 365) return "P2";
        return "P3";
    }

    private static int pageNo(Integer v) { return v == null || v <= 0 ? 1 : v; }
    private static int pageSize(Integer v) { int s = v == null || v <= 0 ? 20 : v; return Math.min(s, 100); }
    private static void put(Map<String, Object> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v.trim());
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
