package nhnis.infra.in.a.application.service;

import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.dto.ifina8300S0DTOin;
import nhnis.infra.in.a.dto.ifina8300S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina8300DAO;

@Service
public class ifina8300Service {
    private final ifina8300DAO dao;

    public ifina8300Service(ifina8300DAO dao) {
        this.dao = dao;
    }

    public ifina8300S0DTOout ifina8300S0(ifina8300S0DTOin input) throws Exception {
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "waveId", input.getWaveId());
            put(param, "strategy7rCd", input.getStrategy7rCd());
            put(param, "targetTypeCd", input.getTargetTypeCd());
            put(param, "difficultyCd", input.getDifficultyCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> raw = dao.ifina8300S0_S0(param);
        if (raw != null) {
            for (Map<String, Object> row : raw) {
                rows.add(mapRow(row));
            }
        }
        int total = dao.ifina8300S0_S0_count(param);
        ifina8300S0DTOout out = new ifina8300S0DTOout();
        out.setRows(rows);
        out.setSize(rows.size());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static Map<String, Object> mapRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        String asis = as(row, "CURRENT_PLATFORM_CD", "currentPlatformCd");
        String tobe = as(row, "TARGET_PLATFORM_CD", "targetPlatformCd");
        String gap = as(row, "GAP_REMARK", "gapRemark");
        String diff = as(row, "DIFFICULTY_CD", "difficultyCd");
        m.put("planId", as(row, "PLAN_ID", "planId"));
        m.put("waveId", as(row, "WAVE_ID", "waveId"));
        m.put("waveName", as(row, "WAVE_NAME", "waveName"));
        m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
        m.put("targetId", as(row, "TARGET_ID", "targetId"));
        m.put("strategy7rCd", as(row, "STRATEGY_7R_CD", "strategy7rCd"));
        m.put("currentPlatformCd", asis);
        m.put("targetPlatformCd", tobe);
        m.put("difficultyCd", diff);
        m.put("statusCd", as(row, "STATUS_CD", "statusCd"));
        m.put("gapRemark", gap);
        m.put("gapHint", buildGapHint(asis, tobe, diff, gap));
        return m;
    }

    static String buildGapHint(String asis, String tobe, String diff, String gap) {
        if (gap != null && !gap.isBlank()) return gap;
        List<String> parts = new ArrayList<>();
        if (asis != null && tobe != null && !asis.equalsIgnoreCase(tobe)) {
            parts.add(asis + "→" + tobe);
        }
        if ("H".equalsIgnoreCase(diff)) {
            parts.add("난이도 H");
        }
        return parts.isEmpty() ? "" : String.join(" / ", parts);
    }

    private static int pageNo(Integer v) { return v == null || v <= 0 ? 1 : v; }
    private static int pageSize(Integer v) { int s = v == null || v <= 0 ? 50 : v; return Math.min(s, 200); }
    private static void put(Map<String, Object> m, String k, String v) { if (v != null && !v.isBlank()) m.put(k, v.trim()); }
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
