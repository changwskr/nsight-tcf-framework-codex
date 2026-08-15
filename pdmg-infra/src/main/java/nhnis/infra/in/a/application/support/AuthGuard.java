package nhnis.infra.in.a.application.support;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import nhnis.infra.in.a.application.rule.InfraAuthRule;

/**
 * RACI HARD 거부 시 DTO에 RSLT_CD=0006을 채우고 change_log에 DENY를 남긴다.
 */
@Component
public class AuthGuard {
    private static final Logger log = LoggerFactory.getLogger(AuthGuard.class);

    private final InfraAuthRule authRule;
    private final ChangeLogWriter changeLogWriter;

    public AuthGuard(InfraAuthRule authRule, ChangeLogWriter changeLogWriter) {
        this.authRule = authRule;
        this.changeLogWriter = changeLogWriter;
    }

    public boolean denyIfHard(Object out, String serviceId) {
        return denyIfHard(out, serviceId, null);
    }

    public boolean denyIfHard(Object out, String serviceId, Map<String, String> attrs) {
        Optional<RuleViolation> hard = authRule.evaluate(serviceId, attrs).firstHard();
        if (hard.isEmpty()) {
            return false;
        }
        RuleViolation h = hard.get();
        invoke(out, "setRSLT_CD", String.class, h.getRsltCd());
        invoke(out, "setRSLT_MSG", String.class, h.formatted());
        invoke(out, "setPROC_CNT", Integer.class, 0);
        auditDeny(serviceId, h);
        return true;
    }

    public InfraAuthRule rule() {
        return authRule;
    }

    private void auditDeny(String serviceId, RuleViolation h) {
        try {
            Map<String, Object> before = new LinkedHashMap<>();
            before.put("role", authRule.resolveRole());
            before.put("mode", authRule.getMode().name());
            before.put("serviceId", serviceId);
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("rsltCd", h.getRsltCd());
            after.put("ruleId", h.getRuleId());
            after.put("msg", h.formatted());
            changeLogWriter.write("RACI", serviceId, "DENY", before, after, "AuthGuard");
        } catch (Exception e) {
            log.warn("RACI DENY audit skipped: {}", e.toString());
        }
    }

    private static void invoke(Object target, String method, Class<?> argType, Object arg) {
        try {
            Method m = target.getClass().getMethod(method, argType);
            m.invoke(target, arg);
        } catch (ReflectiveOperationException ignored) {
            // DTO에 해당 setter 없으면 skip
        }
    }
}
