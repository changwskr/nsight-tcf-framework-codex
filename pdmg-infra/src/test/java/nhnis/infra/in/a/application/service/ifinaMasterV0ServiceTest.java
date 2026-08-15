package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina1300C0DTOin;
import nhnis.infra.in.a.dto.ifina1300S0DTOin;
import nhnis.infra.in.a.dto.ifina1300U0DTOin;
import nhnis.infra.in.a.dto.ifina1400S0DTOin;
import nhnis.infra.in.a.dto.ifina1400U0DTOin;
import nhnis.infra.in.a.dto.ifina6100S0DTOin;
import nhnis.infra.in.a.dto.ifina6200S0DTOin;
import nhnis.infra.in.a.dto.ifina8200S0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaMasterV0ServiceTest {

    @Autowired private ifina1300Service checklistMaster;
    @Autowired private ifina1400Service gateDefService;
    @Autowired private ifina6100Service haService;
    @Autowired private ifina6200Service capacityService;
    @Autowired private ifina8200Service waveService;

    @Test
    @Order(1)
    void checklistMasterListAndDeactivate() throws Exception {
        ifina1300S0DTOin q = new ifina1300S0DTOin();
        q.setActiveYn("Y");
        var list = checklistMaster.ifina1300S0(q);
        assertThat(list.getRSLT_CD()).isEqualTo("0000");
        assertThat(list.getTotalCount()).isGreaterThanOrEqualTo(5);

        ifina1300C0DTOin c = new ifina1300C0DTOin();
        c.setChecklistId("CL-TEST-01");
        c.setItemName("테스트 문항");
        c.setCategoryKo("전환");
        c.setSeverityCd("P2");
        assertThat(checklistMaster.ifina1300C0(c).getRSLT_CD()).isEqualTo("0000");

        ifina1300U0DTOin u = new ifina1300U0DTOin();
        u.setChecklistId("CL-TEST-01");
        u.setItemName("테스트 문항");
        u.setCategoryKo("전환");
        u.setActiveYn("N");
        assertThat(checklistMaster.ifina1300U0(u).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    @Order(2)
    void gateDefListAndUpdate() throws Exception {
        ifina1400S0DTOin q = new ifina1400S0DTOin();
        var list = gateDefService.ifina1400S0(q);
        assertThat(list.getTotalCount()).isGreaterThanOrEqualTo(7);
        assertThat(list.getRows().stream().anyMatch(r -> "GATE6".equals(r.get("gateId")))).isTrue();

        ifina1400U0DTOin u = new ifina1400U0DTOin();
        u.setGateId("GATE6");
        u.setNameKo("Cost Approved");
        u.setDescription("비용·TCO Snapshot 승인");
        u.setSortNo(60);
        u.setActiveYn("Y");
        assertThat(gateDefService.ifina1400U0(u).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    @Order(3)
    void v0EndpointsReturnViolationsShape() throws Exception {
        ifina6100S0DTOin ha = new ifina6100S0DTOin();
        ha.setTargetTypeCd("GROUP");
        ha.setTargetId("SG-BATCH-01");
        var haV0 = haService.ifina6100V0(ha);
        assertThat(haV0.getRSLT_CD()).isEqualTo("0000");
        assertThat(haV0.getWarnCount() + haV0.getErrorCount()).isGreaterThan(0);
        assertThat(haV0.getViolations().get(0)).containsKeys("ruleId", "severity", "message");

        ifina6200S0DTOin cap = new ifina6200S0DTOin();
        cap.setTargetTypeCd("GROUP");
        cap.setTargetId("SG-WAS-A");
        var capV0 = capacityService.ifina6200V0(cap);
        assertThat(capV0.getRSLT_CD()).isEqualTo("0000");
        assertThat(capV0.getViolations()).isNotNull();

        var waveV0 = waveService.ifina8200V0(new ifina8200S0DTOin());
        assertThat(waveV0.getRSLT_CD()).isEqualTo("0000");
        assertThat(waveV0.getViolations()).isNotNull();
    }
}
