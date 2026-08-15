package nhnis.infra.in.a.application.rule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;

class LifecycleTransitionRuleTest {

    @AfterEach
    void clear() {
        ServiceContextHolder.setInstance(null);
    }

    @Test
    void adjacentForwardAllowedForOps() {
        bind("E0000002");
        var rule = LifecycleTransitionRule.forUnitTest(InfraAuthRule.forUnitTest("hard", "OPS"));
        assertThat(rule.evaluate("DISCOVERED", "VALIDATING").hasHard()).isFalse();
        assertThat(rule.evaluate("CONFIRMED", "TARGET_DEFINED").hasHard()).isFalse();
    }

    @Test
    void reverseBlocked() {
        var rule = LifecycleTransitionRule.forUnitTest(InfraAuthRule.forUnitTest("soft", "ARCH"));
        var vr = rule.evaluate("CONFIRMED", "DISCOVERED");
        assertThat(vr.hasHard()).isTrue();
        assertThat(vr.firstHard().orElseThrow().getRuleId()).isEqualTo("RL-LF-002");
    }

    @Test
    void jumpBlockedForOps() {
        bind("E0000002");
        var rule = LifecycleTransitionRule.forUnitTest(InfraAuthRule.forUnitTest("hard", "OPS"));
        var vr = rule.evaluate("DISCOVERED", "CONFIRMED");
        assertThat(vr.hasHard()).isTrue();
        assertThat(vr.firstHard().orElseThrow().getRuleId()).isEqualTo("RL-AU-002");
        assertThat(vr.firstHard().orElseThrow().getRsltCd()).isEqualTo("0006");
    }

    @Test
    void jumpAllowedForArch() {
        bind("E0000001");
        var rule = LifecycleTransitionRule.forUnitTest(InfraAuthRule.forUnitTest("hard", "ARCH"));
        assertThat(rule.evaluate("DISCOVERED", "MIGRATED").hasHard()).isFalse();
    }

    @Test
    void terminalRetireJumpAllowedWhenFlagged() {
        bind("E0000002");
        var rule = LifecycleTransitionRule.forUnitTest(InfraAuthRule.forUnitTest("hard", "OPS"));
        assertThat(rule.evaluate("CONFIRMED", "RETIRED", true).hasHard()).isFalse();
        assertThat(rule.evaluate("CONFIRMED", "RETIRED", false).hasHard()).isTrue();
    }

    private static void bind(String optr) {
        sys_comm sys = new sys_comm();
        sys.setOptr_eno(optr);
        hdr_nhnis hdr = new hdr_nhnis();
        hdr.setSys_comm(sys);
        ServiceContextHolder.setInstance(
                new ServiceContext("pdmg-infra", "GUID", "local", null, null, null, hdr));
    }
}
