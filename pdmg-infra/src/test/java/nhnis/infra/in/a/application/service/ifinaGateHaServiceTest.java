package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina6100S0DTOin;
import nhnis.infra.in.a.dto.ifina6100U0DTOin;
import nhnis.infra.in.a.dto.ifina6200U0DTOin;
import nhnis.infra.in.a.dto.ifina9200S0DTOin;
import nhnis.infra.in.a.dto.ifina9200U0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaGateHaServiceTest {

    @Autowired private ifina9200Service gateService;
    @Autowired private ifina6100Service haService;
    @Autowired private ifina6200Service capacityService;

    @Test
    @Order(1)
    void gateStatusSeedAndRejectPassWithoutHa() throws Exception {
        ifina9200S0DTOin q = new ifina9200S0DTOin();
        q.setTargetTypeCd("GROUP");
        q.setTargetId("SG-WAS-A");
        var status = gateService.ifina9200S0(q);
        assertThat(status.getGates()).hasSize(7);
        assertThat(status.getFailCount() + status.getConditionalCount()).isGreaterThanOrEqualTo(1);

        // 프로파일 없는 대상 → GATE5 PASS HARD
        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE5");
        pass.setTargetTypeCd("GROUP");
        pass.setTargetId("SG-BATCH-01");
        pass.setResultCd("PASS");
        var rejected = gateService.ifina9200U0(pass);
        assertThat(rejected.getRSLT_CD()).isEqualTo("0001");
        assertThat(rejected.getRSLT_MSG()).contains("RL-GT-002");
    }

    @Test
    @Order(2)
    void conditionalRequiresEvidenceThenOk() throws Exception {
        ifina9200U0DTOin noEv = new ifina9200U0DTOin();
        noEv.setGateId("GATE3");
        noEv.setTargetTypeCd("GROUP");
        noEv.setTargetId("SG-WAS-B");
        noEv.setResultCd("CONDITIONAL");
        assertThat(gateService.ifina9200U0(noEv).getRSLT_CD()).isEqualTo("0001");

        ifina9200U0DTOin ok = new ifina9200U0DTOin();
        ok.setGateId("GATE3");
        ok.setTargetTypeCd("GROUP");
        ok.setTargetId("SG-WAS-B");
        ok.setResultCd("CONDITIONAL");
        ok.setEvidence("meeting-2026-08-15");
        ok.setRemark("Peak TPS 후속");
        var saved = gateService.ifina9200U0(ok);
        assertThat(saved.getRSLT_CD()).isEqualTo("0000");
        assertThat(saved.getWarnings().stream().anyMatch(w -> w.contains("RL-GT-005"))).isTrue();
    }

    @Test
    @Order(3)
    void gate1PassRequiresChecklist() throws Exception {
        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE1");
        pass.setTargetTypeCd("ASSET");
        pass.setTargetId("INF-APP-001");
        pass.setResultCd("PASS");
        var rejected = gateService.ifina9200U0(pass);
        assertThat(rejected.getRSLT_CD()).isEqualTo("0001");
        assertThat(rejected.getRSLT_MSG()).contains("RL-GT-001");
    }

    @Test
    @Order(4)
    void haProfileSoftSaveAndGate5Ready() throws Exception {
        ifina6100S0DTOin q = new ifina6100S0DTOin();
        q.setTargetTypeCd("GROUP");
        q.setTargetId("SG-WAS-A");
        var before = haService.ifina6100S0(q);
        assertThat(before.getRSLT_CD()).isEqualTo("0000");
        assertThat(before.getWarnings()).isNotEmpty();

        ifina6100U0DTOin save = new ifina6100U0DTOin();
        save.setTargetTypeCd("GROUP");
        save.setTargetId("SG-WAS-A");
        save.setHaYn("Y");
        save.setHaModeCd("ACTIVE_ACTIVE");
        save.setDrYn("Y");
        save.setRtoMinutes(60);
        save.setRpoMinutes(5);
        save.setOpsHoursCd("24X365");
        assertThat(haService.ifina6100U0(save).getRSLT_CD()).isEqualTo("0000");

        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE5");
        pass.setTargetTypeCd("GROUP");
        pass.setTargetId("SG-WAS-A");
        pass.setResultCd("PASS");
        assertThat(gateService.ifina9200U0(pass).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    @Order(5)
    void gate6PassRequiresCostThenOk() throws Exception {
        ifina9200U0DTOin fail = new ifina9200U0DTOin();
        fail.setGateId("GATE6");
        fail.setTargetTypeCd("SYSTEM");
        fail.setTargetId("SYS-UNKNOWN");
        fail.setResultCd("PASS");
        var rejected = gateService.ifina9200U0(fail);
        assertThat(rejected.getRSLT_CD()).isEqualTo("0001");
        assertThat(rejected.getRSLT_MSG()).contains("RL-GT-006");

        ifina9200U0DTOin ok = new ifina9200U0DTOin();
        ok.setGateId("GATE6");
        ok.setTargetTypeCd("SYSTEM");
        ok.setTargetId("SYS-ONLINE");
        ok.setResultCd("PASS");
        assertThat(gateService.ifina9200U0(ok).getRSLT_CD()).isEqualTo("0000");

        ifina9200S0DTOin q = new ifina9200S0DTOin();
        q.setGateId("GATE6");
        q.setTargetTypeCd("GROUP");
        q.setTargetId("SG-WAS-A");
        var status = gateService.ifina9200S0(q);
        // GROUP → SYS-ONLINE 비용 시드로 Soft 경고 없음
        assertThat(status.getHints().stream().noneMatch(h -> h.contains("RL-GT-006"))).isTrue();
    }

    @Test
    @Order(6)
    void gate3PassElevatesCapacitySoftToHard() throws Exception {
        // 다른 테스트가 N1을 상향했을 수 있어 시드 위반 상태로 복원
        ifina6200U0DTOin n1 = new ifina6200U0DTOin();
        n1.setTargetTypeCd("GROUP");
        n1.setTargetId("SG-WAS-A");
        n1.setMetricScopeCd("N1");
        n1.setTps(new java.math.BigDecimal("400"));
        n1.setCpuPct(new java.math.BigDecimal("83"));
        n1.setMemPct(new java.math.BigDecimal("76"));
        assertThat(capacityService.ifina6200U0(n1).getRSLT_CD()).isEqualTo("0000");

        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE3");
        pass.setTargetTypeCd("GROUP");
        pass.setTargetId("SG-WAS-A");
        pass.setResultCd("PASS");
        var rejected = gateService.ifina9200U0(pass);
        assertThat(rejected.getRSLT_CD()).isEqualTo("0001");
        assertThat(rejected.getRSLT_MSG()).contains("RL-CP-001");
    }

    @Test
    @Order(7)
    void gate5PassRejectsWithoutSecurityProfile() throws Exception {
        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE5");
        pass.setTargetTypeCd("GROUP");
        pass.setTargetId("SG-BATCH-01");
        pass.setResultCd("PASS");
        var rejected = gateService.ifina9200U0(pass);
        assertThat(rejected.getRSLT_CD()).isEqualTo("0001");
        // HA 또는 Security 중 먼저 HARD
        assertThat(rejected.getRSLT_MSG()).matches("(?s).*(RL-GT-002|RL-SC-001).*");
    }
}
