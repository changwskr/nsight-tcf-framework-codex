package nhnis.infra.in.a.application.rule;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import nhnis.infra.in.a.application.support.RuleViolation;
import nhnis.infra.in.a.application.support.ValidationResult;

/**
 * RL-LF-002 역행 금지, RL-AU-002 임의 점프는 Arch/Admin만.
 * <pre>
 * DISCOVERED → VALIDATING → CONFIRMED → TARGET_DEFINED
 *   → MIGRATION_PLANNED → MIGRATED → RETIRED
 * </pre>
 */
@Component
public class LifecycleTransitionRule {

    private static final List<String> ORDER = List.of(
            "DISCOVERED",
            "VALIDATING",
            "CONFIRMED",
            "TARGET_DEFINED",
            "MIGRATION_PLANNED",
            "MIGRATED",
            "RETIRED");

    private final InfraAuthRule authRule;

    public LifecycleTransitionRule(InfraAuthRule authRule) {
        this.authRule = authRule;
    }

    /** 단위테스트용. */
    public static LifecycleTransitionRule forUnitTest(InfraAuthRule authRule) {
        return new LifecycleTransitionRule(authRule);
    }

    public ValidationResult evaluate(String fromStatus, String toStatus) {
        return evaluate(fromStatus, toStatus, false);
    }

    /**
     * @param allowTerminalRetire true면 임의 상태에서 RETIRED로 폐기(D0) 허용 (점프 AU-002 면제)
     */
    public ValidationResult evaluate(String fromStatus, String toStatus, boolean allowTerminalRetire) {
        ValidationResult r = new ValidationResult();
        String from = normalize(fromStatus);
        String to = normalize(toStatus);
        if (to == null || from == null || from.equals(to)) {
            return r;
        }
        int fi = ORDER.indexOf(from);
        int ti = ORDER.indexOf(to);
        if (fi < 0 || ti < 0) {
            r.add(RuleViolation.soft("RL-LF-002",
                    "알 수 없는 Lifecycle 상태 (from=" + from + ", to=" + to + ")"));
            return r;
        }
        if (ti < fi) {
            r.add(RuleViolation.hard("RL-LF-002", "0005",
                    "Lifecycle 역행 금지 (" + from + "→" + to + ")"));
            return r;
        }
        if (ti > fi + 1) {
            if (allowTerminalRetire && "RETIRED".equals(to)) {
                return r;
            }
            String role = authRule.resolveRole();
            if (!Set.of("ARCH", "ADMIN").contains(role)) {
                r.add(RuleViolation.hard("RL-AU-002", "0006",
                        "Lifecycle 점프는 Arch/Admin만 허용 (" + from + "→" + to + ", role=" + role + ")"));
            }
        }
        return r;
    }

    private static String normalize(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }
}
