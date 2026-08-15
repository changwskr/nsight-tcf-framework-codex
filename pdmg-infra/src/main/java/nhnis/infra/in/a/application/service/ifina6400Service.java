package nhnis.infra.in.a.application.service;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.dto.ifina6400S0DTOin;
import nhnis.infra.in.a.dto.ifina6400S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina6400DAO;

@Service
public class ifina6400Service {
    private static final List<String> METRIC_KEYS = List.of("cpuPct", "memPct", "tps", "respP95Ms", "dbConnPeak");
    private static final Map<String, String> METRIC_LABELS = Map.of(
            "cpuPct", "CPU %", "memPct", "Mem %", "tps", "TPS",
            "respP95Ms", "Resp P95 ms", "dbConnPeak", "DB Conn Peak");

    private final ifina6400DAO dao;

    public ifina6400Service(ifina6400DAO dao) {
        this.dao = dao;
    }

    public ifina6400S0DTOout ifina6400S0(ifina6400S0DTOin input) throws Exception {
        ifina6400S0DTOout out = new ifina6400S0DTOout();
        String targetType = blank(input == null ? null : input.getTargetTypeCd(), "GROUP");
        String scope = blank(input == null ? null : input.getMetricScopeCd(), "PEAK").toUpperCase(Locale.ROOT);
        List<String> ids = normalizeIds(input == null ? null : input.getTargetIdList());
        if (ids.isEmpty()) {
            ids = List.of("SG-WAS-A", "SG-WAS-B");
        }
        if (ids.size() > 10) {
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("targetIdList max 10");
            return out;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("targetTypeCd", targetType);
        param.put("metricScopeCd", scope);
        param.put("targetIdList", ids);
        List<Map<String, Object>> raw = dao.ifina6400S0_S0(param);

        Map<String, Map<String, Object>> byTarget = new LinkedHashMap<>();
        for (String id : ids) {
            byTarget.put(id, null);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                Map<String, Object> m = mapRow(row);
                String tid = str(m.get("targetId"));
                if (tid != null) {
                    byTarget.put(tid, m);
                }
                rows.add(m);
            }
        }

        List<Map<String, Object>> metrics = new ArrayList<>();
        for (String key : METRIC_KEYS) {
            Map<String, Object> metric = new LinkedHashMap<>();
            metric.put("metricKey", key);
            metric.put("metricLabel", METRIC_LABELS.get(key));
            Map<String, Object> values = new LinkedHashMap<>();
            BigDecimal max = null;
            String maxTarget = null;
            for (String id : ids) {
                Map<String, Object> snap = byTarget.get(id);
                Object val = snap == null ? null : snap.get(key);
                values.put(id, val);
                BigDecimal bd = toBd(val);
                if (bd != null && (max == null || bd.compareTo(max) > 0)) {
                    max = bd;
                    maxTarget = id;
                }
            }
            metric.put("values", values);
            metric.put("maxTargetId", maxTarget);
            metric.put("maxValue", max);
            metrics.add(metric);
        }

        out.setTargetTypeCd(targetType);
        out.setMetricScopeCd(scope);
        out.setTargetIds(ids);
        out.setRows(rows);
        out.setMetrics(metrics);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static List<String> normalizeIds(List<String> raw) {
        if (raw == null) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String v : raw) {
            if (v == null) continue;
            for (String part : v.split("[,\\s]+")) {
                String t = part.trim();
                if (!t.isEmpty()) set.add(t);
            }
        }
        return new ArrayList<>(set);
    }

    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("snapshotId", as(row, "SNAPSHOT_ID", "snapshotId"));
        m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
        m.put("targetId", as(row, "TARGET_ID", "targetId"));
        m.put("metricScopeCd", as(row, "METRIC_SCOPE_CD", "metricScopeCd"));
        m.put("capturedAt", as(row, "CAPTURED_AT", "capturedAt"));
        m.put("cpuPct", toBd(val(row, "CPU_PCT", "cpuPct")));
        m.put("memPct", toBd(val(row, "MEM_PCT", "memPct")));
        m.put("tps", toBd(val(row, "TPS", "tps")));
        m.put("respP95Ms", toBd(val(row, "RESP_P95_MS", "respP95Ms")));
        Object db = val(row, "DB_CONN_PEAK", "dbConnPeak");
        Integer dbPeak = null;
        if (db != null) {
            try { dbPeak = new BigDecimal(String.valueOf(db)).intValue(); } catch (Exception ignored) { }
        }
        m.put("dbConnPeak", dbPeak);
        return m;
    }

    private static Object val(Map<String, Object> row, String u, String c) {
        Object v = row.get(u);
        if (v == null) v = row.get(c);
        if (v == null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && (e.getKey().equalsIgnoreCase(u) || e.getKey().equalsIgnoreCase(c))) {
                    return e.getValue();
                }
            }
        }
        return v;
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return null;
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception e) { return null; }
    }
    private static String blank(String v, String d) {
        if (v == null || v.isBlank()) return d;
        return v.trim();
    }
    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
    private static String as(Map<String, Object> row, String u, String c) {
        Object v = val(row, u, c);
        return v == null ? null : String.valueOf(v);
    }
}
