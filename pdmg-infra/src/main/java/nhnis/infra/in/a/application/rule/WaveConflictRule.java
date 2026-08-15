package nhnis.infra.in.a.application.rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.ValidationResult;
import nhnis.infra.in.a.persistence.dao.ifina8200DAO;

/**
 * RL-MG-004: Critical 의존관계의 Wave 불일치.
 */
@Component
public class WaveConflictRule {
    private final ifina8200DAO dao;

    public WaveConflictRule(ifina8200DAO dao) {
        this.dao = dao;
    }

    public ValidationResult evaluate() throws Exception {
        ValidationResult r = new ValidationResult();
        Map<String, String> targetWave = new HashMap<>();
        List<Map<String, Object>> plans = dao.ifina8200S0_plans(Map.of());
        if (plans != null) {
            for (Map<String, Object> p : plans) {
                String tid = as(p, "TARGET_ID", "targetId");
                String wid = as(p, "WAVE_ID", "waveId");
                if (tid != null && wid != null && !wid.isBlank()) {
                    targetWave.put(tid, wid);
                }
            }
        }
        List<Map<String, Object>> rels = dao.ifina8200S0_criticalRels(Map.of());
        if (rels == null) return r;
        for (Map<String, Object> row : rels) {
            String from = as(row, "FROM_ID", "fromId");
            String to = as(row, "TO_ID", "toId");
            String wf = targetWave.get(from);
            String wt = targetWave.get(to);
            if (wf != null && wt != null && !wf.equals(wt)) {
                String relId = as(row, "RELATION_ID", "relationId");
                r.add(RuleViolation.soft("RL-MG-004",
                        "Critical " + relId + " " + from + "(" + wf + ")→" + to + "(" + wt + ")"));
            }
        }
        return r;
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
