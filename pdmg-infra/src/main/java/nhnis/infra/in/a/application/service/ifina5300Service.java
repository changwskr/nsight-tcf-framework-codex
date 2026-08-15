package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.ifina5300C0DTOin;
import nhnis.infra.in.a.dto.ifina5300C0DTOout;
import nhnis.infra.in.a.dto.ifina5300D0DTOin;
import nhnis.infra.in.a.dto.ifina5300D0DTOout;
import nhnis.infra.in.a.dto.ifina5300S0DTOin;
import nhnis.infra.in.a.dto.ifina5300S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina5300DAO;

@Service
public class ifina5300Service {
    private final ifina5300DAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final AuthGuard authGuard;

    public ifina5300Service(ifina5300DAO dao, ChangeLogWriter changeLogWriter, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
    }

    public ifina5300S0DTOout ifina5300S0(ifina5300S0DTOin input) throws Exception {
        String rootId = trim(input == null ? null : input.getRootId());
        int depth = input != null && input.getDepth() != null && input.getDepth() > 0
                ? Math.min(input.getDepth(), 5) : 1;
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "fromTypeCd", input.getFromTypeCd());
            put(param, "fromId", input.getFromId());
            put(param, "toTypeCd", input.getToTypeCd());
            put(param, "toId", input.getToId());
            put(param, "relationTypeCd", input.getRelationTypeCd());
            put(param, "criticalYn", input.getCriticalYn());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());

        List<Map<String, Object>> edges = new ArrayList<>();
        if (rootId != null) {
            edges = bfs(rootId, depth, param);
        } else {
            param.put("offset", (pageNo - 1) * pageSize);
            param.put("pageSize", pageSize);
            List<Map<String, Object>> raw = dao.ifina5300S0_S0(param);
            if (raw != null) {
                for (Map<String, Object> row : raw) {
                    edges.add(mapEdge(row));
                }
            }
        }

        LinkedHashMap<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        for (Map<String, Object> e : edges) {
            putNode(nodeMap, str(e.get("fromTypeCd")), str(e.get("fromId")));
            putNode(nodeMap, str(e.get("toTypeCd")), str(e.get("toId")));
        }
        if (rootId != null && input != null) {
            putNode(nodeMap, blank(input.getRootType(), "ASSET"), rootId);
        }

        ifina5300S0DTOout out = new ifina5300S0DTOout();
        out.setRootType(input == null ? null : input.getRootType());
        out.setRootId(rootId);
        out.setDepth(depth);
        out.setEdges(edges);
        out.setNodes(new ArrayList<>(nodeMap.values()));
        out.setRows(edges);
        out.setSize(edges.size());
        if (rootId != null) {
            out.setPageNo(1);
            out.setPageSize(edges.size());
            out.setTotalCount(edges.size());
            out.setTotalPages(1);
        } else {
            int total = dao.ifina5300S0_S0_count(param);
            out.setPageNo(pageNo);
            out.setPageSize(pageSize);
            out.setTotalCount(total);
            out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        }
        return out;
    }

    public ifina5300C0DTOout ifina5300C0(ifina5300C0DTOin input) throws Exception {
        ifina5300C0DTOout out = new ifina5300C0DTOout();
        if (authGuard.denyIfHard(out, "ifina5300C0")) return out;
        String relationId = trim(input == null ? null : input.getRelationId());
        String fromId = trim(input == null ? null : input.getFromId());
        String toId = trim(input == null ? null : input.getToId());
        String relType = trim(input == null ? null : input.getRelationTypeCd());
        if (relationId == null || fromId == null || toId == null || relType == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: relationId, fromId, toId, relationTypeCd");
            return out;
        }
        if (dao.ifina5300S0_S0_exists(Map.of("relationId", relationId)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_RELATION_ID");
            return out;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("relationId", relationId);
        p.put("fromTypeCd", blank(input.getFromTypeCd(), "ASSET"));
        p.put("fromId", fromId);
        p.put("toTypeCd", blank(input.getToTypeCd(), "ASSET"));
        p.put("toId", toId);
        p.put("relationTypeCd", relType);
        p.put("criticalYn", blank(input.getCriticalYn(), "N").toUpperCase(Locale.ROOT));
        p.put("remark", empty(input.getRemark()));
        p.put("regUserId", blank(input.getRegUserId(), "LOCAL"));
        p.put("regDtm", now());
        out.setPROC_CNT(dao.ifina5300C0_C0(p));
        changeLogWriter.write("RELATION", relationId, "CREATE", null, p, "ifina5300C0");
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    public ifina5300D0DTOout ifina5300D0(ifina5300D0DTOin input) throws Exception {
        ifina5300D0DTOout out = new ifina5300D0DTOout();
        if (authGuard.denyIfHard(out, "ifina5300D0")) return out;
        if (input == null || input.getRelationIdList() == null || input.getRelationIdList().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_DATA");
            return out;
        }
        List<String> ids = input.getRelationIdList().stream()
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
        out.setPROC_CNT(dao.ifina5300D0_D0(Map.of("relationIdList", ids)));
        for (String id : ids) {
            changeLogWriter.write("RELATION", id, "DELETE", Map.of("relationId", id), null, "ifina5300D0");
        }
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private List<Map<String, Object>> bfs(String rootId, int depth, Map<String, Object> baseFilter) throws Exception {
        Map<String, Object> allQ = new HashMap<>(baseFilter);
        allQ.remove("rootId");
        allQ.put("offset", 0);
        allQ.put("pageSize", 500);
        List<Map<String, Object>> all = dao.ifina5300S0_S0(allQ);
        if (all == null) {
            return List.of();
        }
        Map<String, List<Map<String, Object>>> adj = new HashMap<>();
        for (Map<String, Object> row : all) {
            String from = as(row, "FROM_ID", "fromId");
            String to = as(row, "TO_ID", "toId");
            if (from == null || to == null) {
                continue;
            }
            adj.computeIfAbsent(from, k -> new ArrayList<>()).add(row);
            adj.computeIfAbsent(to, k -> new ArrayList<>()).add(row);
        }
        Set<String> visitedNodes = new HashSet<>();
        Set<String> visitedEdges = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        Queue<String> q = new ArrayDeque<>();
        Queue<Integer> d = new ArrayDeque<>();
        q.add(rootId);
        d.add(0);
        visitedNodes.add(rootId);
        while (!q.isEmpty()) {
            String cur = q.poll();
            int curDepth = d.poll();
            if (curDepth >= depth) {
                continue;
            }
            for (Map<String, Object> row : adj.getOrDefault(cur, List.of())) {
                String rid = as(row, "RELATION_ID", "relationId");
                if (rid == null || !visitedEdges.add(rid)) {
                    continue;
                }
                result.add(mapEdge(row));
                String from = as(row, "FROM_ID", "fromId");
                String to = as(row, "TO_ID", "toId");
                String next = cur.equals(from) ? to : (cur.equals(to) ? from : null);
                if (next != null && visitedNodes.add(next)) {
                    q.add(next);
                    d.add(curDepth + 1);
                }
            }
        }
        return result;
    }

    private static void putNode(Map<String, Map<String, Object>> map, String type, String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        String key = (type == null ? "ASSET" : type) + "|" + id;
        map.computeIfAbsent(key, k -> {
            Map<String, Object> n = new HashMap<>();
            n.put("nodeType", type == null ? "ASSET" : type);
            n.put("nodeId", id);
            n.put("label", id);
            return n;
        });
    }

    private static Map<String, Object> mapEdge(Map<String, Object> row) {
        Map<String, Object> m = new HashMap<>();
        m.put("relationId", as(row, "RELATION_ID", "relationId"));
        m.put("fromTypeCd", as(row, "FROM_TYPE_CD", "fromTypeCd"));
        m.put("fromId", as(row, "FROM_ID", "fromId"));
        m.put("toTypeCd", as(row, "TO_TYPE_CD", "toTypeCd"));
        m.put("toId", as(row, "TO_ID", "toId"));
        m.put("relationTypeCd", as(row, "RELATION_TYPE_CD", "relationTypeCd"));
        m.put("criticalYn", as(row, "CRITICAL_YN", "criticalYn"));
        m.put("remark", as(row, "REMARK", "remark"));
        return m;
    }

    private static int pageNo(Integer v) { return v == null || v <= 0 ? 1 : v; }
    private static int pageSize(Integer v) { int s = v == null || v <= 0 ? 50 : v; return Math.min(s, 200); }
    private static String now() { return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()); }
    private static String trim(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private static String empty(String v) { return v == null ? "" : v.trim(); }
    private static String blank(String v, String d) { String t = trim(v); return t != null ? t : d; }
    private static void put(Map<String, Object> m, String k, String v) { if (v != null && !v.isBlank()) m.put(k, v.trim()); }
    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
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
