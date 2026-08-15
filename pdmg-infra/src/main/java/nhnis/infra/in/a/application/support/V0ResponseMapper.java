package nhnis.infra.in.a.application.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nhnis.infra.in.a.dto.ifinaV0DTOout;

public final class V0ResponseMapper {
    private V0ResponseMapper() {}

    public static ifinaV0DTOout from(ValidationResult vr) {
        ifinaV0DTOout out = new ifinaV0DTOout();
        List<Map<String, Object>> rows = new ArrayList<>();
        int warn = 0, err = 0;
        if (vr != null) {
            for (RuleViolation v : vr.getViolations()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ruleId", v.getRuleId());
                m.put("severity", v.getSeverity().name());
                m.put("target", null);
                m.put("message", v.getMessage());
                rows.add(m);
                if (v.getSeverity() == RuleViolation.Severity.HARD) err++;
                else warn++;
            }
        }
        out.setViolations(rows);
        out.setWarnCount(warn);
        out.setErrorCount(err);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(err == 0 && warn == 0 ? "OK" : "errors=" + err + ", warns=" + warn);
        return out;
    }

    public static ifinaV0DTOout from(ValidationResult vr, String target) {
        ifinaV0DTOout out = from(vr);
        if (target != null) {
            for (Map<String, Object> m : out.getViolations()) {
                m.put("target", target);
            }
        }
        return out;
    }
}
