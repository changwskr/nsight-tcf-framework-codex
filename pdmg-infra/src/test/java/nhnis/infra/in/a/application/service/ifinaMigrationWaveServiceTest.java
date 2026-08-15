package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina8100C0DTOin;
import nhnis.infra.in.a.dto.ifina8100D0DTOin;
import nhnis.infra.in.a.dto.ifina8100S0DTOin;
import nhnis.infra.in.a.dto.ifina8200S0DTOin;
import nhnis.infra.in.a.dto.ifina8200U0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaMigrationWaveServiceTest {

    @Autowired private ifina8100Service planService;
    @Autowired private ifina8200Service waveService;

    @Test
    @Order(1)
    void planListShowsUnassignedSoft() throws Exception {
        ifina8100S0DTOin q = new ifina8100S0DTOin();
        q.setPageNo(1);
        q.setPageSize(50);
        var out = planService.ifina8100S0(q);
        assertThat(out.getTotalCount()).isGreaterThanOrEqualTo(5);
        assertThat(out.getWarnings().stream().anyMatch(w -> w.contains("RL-MG-003"))).isTrue();
    }

    @Test
    @Order(2)
    void planRejectsMissingTargetPlatform() throws Exception {
        ifina8100C0DTOin create = new ifina8100C0DTOin();
        create.setPlanId("MP-BAD-001");
        create.setTargetTypeCd("GROUP");
        create.setTargetId("SG-WAS-A");
        create.setStrategy7rCd("REHOST");
        assertThat(planService.ifina8100C0(create).getRSLT_MSG()).contains("RL-MG-001");
    }

    @Test
    @Order(3)
    void planCreateUpdateDelete() throws Exception {
        ifina8100C0DTOin create = new ifina8100C0DTOin();
        create.setPlanId("MP-TEST-001");
        create.setTargetTypeCd("GROUP");
        create.setTargetId("SG-WAS-B");
        create.setStrategy7rCd("REHOST");
        create.setTargetPlatformCd("IAAS");
        create.setWaveId("W1");
        assertThat(planService.ifina8100C0(create).getRSLT_CD()).isEqualTo("0000");

        ifina8100D0DTOin del = new ifina8100D0DTOin();
        del.setPlanIdList(List.of("MP-TEST-001"));
        assertThat(planService.ifina8100D0(del).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    @Order(4)
    void waveListShowsCrossWaveCriticalSoft() throws Exception {
        var out = waveService.ifina8200S0(new ifina8200S0DTOin());
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getRows()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(out.getWarnings().stream().anyMatch(w -> w.contains("RL-MG-004"))).isTrue();
    }

    @Test
    @Order(5)
    void waveUpsert() throws Exception {
        ifina8200U0DTOin save = new ifina8200U0DTOin();
        save.setWaveId("W1");
        save.setWaveName("저위험·공통(갱신)");
        save.setSequenceNo(1);
        save.setStatusCd("PLANNED");
        assertThat(waveService.ifina8200U0(save).getRSLT_CD()).isEqualTo("0000");
    }
}
