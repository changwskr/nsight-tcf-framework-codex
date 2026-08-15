package nhnis.infra.in.a.application.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * InfraIntegrityValidator 결과.
 */
public class ValidationResult {
    private final List<RuleViolation> violations = new ArrayList<>();

    public void add(RuleViolation v) {
        if (v != null) {
            violations.add(v);
        }
    }

    public void addAll(ValidationResult other) {
        if (other != null) {
            violations.addAll(other.violations);
        }
    }

    public boolean hasHard() {
        return violations.stream().anyMatch(v -> v.getSeverity() == RuleViolation.Severity.HARD);
    }

    public Optional<RuleViolation> firstHard() {
        return violations.stream().filter(v -> v.getSeverity() == RuleViolation.Severity.HARD).findFirst();
    }

    public List<String> softWarnings() {
        return violations.stream()
                .filter(v -> v.getSeverity() != RuleViolation.Severity.HARD)
                .map(RuleViolation::formatted)
                .toList();
    }

    public List<RuleViolation> getViolations() {
        return List.copyOf(violations);
    }
}
