package nhnis.infra.in.a.application.service;

import java.util.*;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.dto.ifina9300S0DTOin;
import nhnis.infra.in.a.dto.ifina9300S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina9300DAO;

@Service
public class ifina9300Service {
    private final ifina9300DAO dao;

    public ifina9300Service(ifina9300DAO dao) {
        this.dao = dao;
    }

    public ifina9300S0DTOout ifina9300S0(ifina9300S0DTOin input) throws Exception {
        String keyword = trim(input == null ? null : input.getKeyword());
        String severity = trim(input == null ? null : input.getSeverityCd());
        String gapType = blank(input == null ? null : input.getGapType(), "ALL").toUpperCase(Locale.ROOT);

        List<Map<String, Object>> rows = new ArrayList<>();
        long checklist = 0, ha = 0, capacity = 0, wave = 0, status = 0;

        if (match(gapType, "CHECKLIST", "ALL")) {
            List<Map<String, Object>> raw = dao.checklistOpen(Map.of());
            if (raw != null) {
                for (Map<String, Object> r : raw) {
                    checklist++;
                    rows.add(item(as(r, "SEVERITY_CD", "severityCd"), "CHECKLIST",
                            as(r, "TARGET_TYPE", "targetType"), as(r, "TARGET_ID", "targetId"),
                            as(r, "ITEM_NAME", "itemName"), "인프라팀", "20260820",
                            as(r, "REMARK", "remark")));
                }
            }
        }
        if (match(gapType, "HA", "ALL")) {
            List<Map<String, Object>> raw = dao.haGaps(Map.of());
            if (raw != null) {
                for (Map<String, Object> r : raw) {
                    ha++;
                    String miss = "N".equalsIgnoreCase(as(r, "HA_YN", "haYn")) ? "HA_YN=N" : "RTO/RPO";
                    rows.add(item("P0", "HA", as(r, "TARGET_TYPE_CD", "targetTypeCd"),
                            as(r, "TARGET_ID", "targetId"), miss, "인프라팀", "20260820", null));
                }
            }
        }
        if (match(gapType, "CAPACITY", "ALL")) {
            List<Map<String, Object>> raw = dao.capacityGaps(Map.of());
            if (raw != null) {
                for (Map<String, Object> r : raw) {
                    capacity++;
                    rows.add(item("P2", "CAPACITY", "GROUP", as(r, "GROUP_ID", "groupId"),
                            "Peak TPS 미수집", "운영팀", "20260830", as(r, "STATUS_CD", "statusCd")));
                }
            }
        }
        if (match(gapType, "WAVE", "ALL")) {
            List<Map<String, Object>> raw = dao.waveGaps(Map.of());
            if (raw != null) {
                for (Map<String, Object> r : raw) {
                    wave++;
                    rows.add(item("P1", "WAVE", as(r, "TARGET_TYPE_CD", "targetTypeCd"),
                            as(r, "TARGET_ID", "targetId"), "Wave 미배정 (" + as(r, "PLAN_ID", "planId") + ")",
                            "PMO", "20260825", as(r, "STATUS_CD", "statusCd")));
                }
            }
        }
        if (match(gapType, "STATUS", "ALL")) {
            List<Map<String, Object>> raw = dao.validatingGroups(Map.of());
            if (raw != null) {
                for (Map<String, Object> r : raw) {
                    status++;
                    rows.add(item("P1", "STATUS", "GROUP", as(r, "GROUP_ID", "groupId"),
                            "상태 " + as(r, "STATUS_CD", "statusCd"), "운영팀", "20260822",
                            as(r, "GROUP_NAME", "groupName")));
                }
            }
        }

        if (keyword != null) {
            String kw = keyword.toUpperCase(Locale.ROOT);
            rows = new ArrayList<>(rows.stream().filter(r ->
                    String.valueOf(r.get("targetId")).toUpperCase(Locale.ROOT).contains(kw)
                            || String.valueOf(r.get("missingItem")).toUpperCase(Locale.ROOT).contains(kw)
            ).toList());
        }
        if (severity != null) {
            String sev = severity;
            rows = new ArrayList<>(rows.stream()
                    .filter(r -> sev.equalsIgnoreCase(String.valueOf(r.get("severityCd")))).toList());
        }
        rows.sort(Comparator
                .comparing((Map<String, Object> r) -> sevRank(String.valueOf(r.get("severityCd"))))
                .thenComparing(r -> String.valueOf(r.get("gapType")))
                .thenComparing(r -> String.valueOf(r.get("targetId"))));

        ifina9300S0DTOout out = new ifina9300S0DTOout();
        out.setRows(rows);
        out.setSize(rows.size());
        out.setChecklistCount(checklist);
        out.setHaCount(ha);
        out.setCapacityCount(capacity);
        out.setWaveCount(wave);
        out.setStatusCount(status);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static Map<String, Object> item(String sev, String type, String targetType, String targetId,
                                            String missing, String owner, String due, String status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("severityCd", sev == null ? "P2" : sev);
        m.put("gapType", type);
        m.put("targetTypeCd", targetType);
        m.put("targetId", targetId);
        m.put("missingItem", missing);
        m.put("statusCd", status == null ? "" : status);
        m.put("ownerOrg", owner);
        m.put("dueDt", due);
        return m;
    }

    private static boolean match(String gapType, String... allowed) {
        for (String a : allowed) {
            if (a.equalsIgnoreCase(gapType)) return true;
        }
        return false;
    }

    private static int sevRank(String s) {
        if ("P0".equalsIgnoreCase(s)) return 0;
        if ("P1".equalsIgnoreCase(s)) return 1;
        if ("P2".equalsIgnoreCase(s)) return 2;
        return 3;
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
                    v = e.getValue();
                    break;
                }
            }
        }
        return v == null ? null : String.valueOf(v);
    }
}
