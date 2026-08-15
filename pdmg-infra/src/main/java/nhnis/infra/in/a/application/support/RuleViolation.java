package nhnis.infra.in.a.application.support;

/**
 * 정합성 위반 1건.
 */
public class RuleViolation {
    public enum Severity { HARD, SOFT, INFO }

    private final String ruleId;
    private final Severity severity;
    private final String rsltCd;
    private final String message;

    public RuleViolation(String ruleId, Severity severity, String rsltCd, String message) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.rsltCd = rsltCd;
        this.message = message;
    }

    public static RuleViolation hard(String ruleId, String rsltCd, String message) {
        return new RuleViolation(ruleId, Severity.HARD, rsltCd, message);
    }

    public static RuleViolation soft(String ruleId, String message) {
        return new RuleViolation(ruleId, Severity.SOFT, "0000", message);
    }

    public String getRuleId() { return ruleId; }
    public Severity getSeverity() { return severity; }
    public String getRsltCd() { return rsltCd; }
    public String getMessage() { return message; }
    public String formatted() { return "[" + ruleId + "] " + message; }
}
