package nhnis.infra.in.a.application.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class InfraAuthRuleTest {

    @Test
    void softModeWarnsPmoOnGate() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("soft", "PMO");
        var vr = rule.evaluate("ifina9200U0");
        assertThat(vr.hasHard()).isFalse();
        assertThat(vr.softWarnings().stream().anyMatch(w -> w.contains("RL-AU-003"))).isTrue();
    }

    @Test
    void hardModeBlocksOpsOnGate() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("hard", "OPS");
        var vr = rule.evaluate("ifina9200U0");
        assertThat(vr.hasHard()).isTrue();
        assertThat(vr.firstHard().orElseThrow().getRsltCd()).isEqualTo("0006");
    }

    @Test
    void offModeSkips() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("off", "OPS");
        assertThat(rule.evaluate("ifina9200U0").getViolations()).isEmpty();
    }

    @Test
    void hardModeBlocksOpsOnCostAndMigration() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("hard", "OPS");
        assertThat(rule.evaluate("ifina7300C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina7100C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina7200U0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina8100C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina8100D0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina8200U0").hasHard()).isTrue();
    }

    @Test
    void hardModeBlocksPmoOnInventoryAndTechWrites() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("hard", "PMO");
        assertThat(rule.evaluate("ifina2100C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina1999U0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina3100D0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina4100C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina5100U0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina6100U0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina9100U0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina3400C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina9200U0").hasHard()).isTrue();
    }

    @Test
    void hardModeMasterRequiresAdminOnly() {
        assertThat(InfraAuthRule.forUnitTest("hard", "OPS").evaluate("ifina1100C0").hasHard()).isTrue();
        assertThat(InfraAuthRule.forUnitTest("hard", "ARCH").evaluate("ifina1100C0").hasHard()).isTrue();
        assertThat(InfraAuthRule.forUnitTest("hard", "ADMIN").evaluate("ifina1100C0").hasHard()).isFalse();
        assertThat(InfraAuthRule.forUnitTest("hard", "SEC").evaluate("ifina1500C0").hasHard()).isTrue();
        assertThat(InfraAuthRule.forUnitTest("hard", "OPS").evaluate("ifina1500E0").hasHard()).isTrue();
        assertThat(InfraAuthRule.forUnitTest("hard", "ADMIN").evaluate("ifina1500E0").hasHard()).isFalse();
    }

    @Test
    void hardModeSecGate5Only() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("hard", "SEC");
        assertThat(rule.evaluate("ifina9200U0", Map.of("gateId", "GATE5")).hasHard()).isFalse();
        assertThat(rule.evaluate("ifina9200U0", Map.of("gateId", "GATE1")).hasHard()).isTrue();
        assertThat(rule.evaluate("ifina6300U0").hasHard()).isFalse();
        assertThat(rule.evaluate("ifina1100C0").hasHard()).isTrue();
    }

    @Test
    void hardModeAllowsPmoOnCost() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("hard", "PMO");
        assertThat(rule.evaluate("ifina7300C0").hasHard()).isFalse();
        assertThat(rule.evaluate("ifina7100C0").hasHard()).isFalse();
        assertThat(rule.evaluate("ifina8100C0").hasHard()).isFalse();
    }

    @Test
    void hardModeDbaOnlyDbDomain() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("hard", "DBA");
        assertThat(rule.evaluate("ifina4200C0").hasHard()).isFalse();
        assertThat(rule.evaluate("ifina4100C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina2100C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina9100U0").hasHard()).isFalse();
        assertThat(rule.evaluate("ifina3100U0", Map.of("techRoleCd", "DATABASE")).hasHard()).isFalse();
        assertThat(rule.evaluate("ifina3100U0", Map.of("techRoleCd", "WAS")).hasHard()).isTrue();
        assertThat(rule.evaluate("ifina3100C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina7300C0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina1100C0").hasHard()).isTrue();
    }

    @Test
    void hardModeMwOnlyMwDomain() {
        InfraAuthRule rule = InfraAuthRule.forUnitTest("hard", "MW");
        assertThat(rule.evaluate("ifina4100U0").hasHard()).isFalse();
        assertThat(rule.evaluate("ifina4200U0").hasHard()).isTrue();
        assertThat(rule.evaluate("ifina3100C0", Map.of("techRoleCd", "WAS")).hasHard()).isFalse();
        assertThat(rule.evaluate("ifina3100C0", Map.of("techRoleCd", "DATABASE")).hasHard()).isTrue();
        assertThat(rule.evaluate("ifina9100C0").hasHard()).isFalse();
        assertThat(rule.evaluate("ifina9200U0").hasHard()).isTrue();
    }
}
