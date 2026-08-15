package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina2300C0DTOin;
import nhnis.infra.in.a.dto.ifina2300D0DTOin;
import nhnis.infra.in.a.dto.ifina2300S0DTOin;
import nhnis.infra.in.a.dto.ifina6200S0DTOin;
import nhnis.infra.in.a.dto.ifina6200U0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaCapacityAppMapServiceTest {

    @Autowired private ifina6200Service capacityService;
    @Autowired private ifina2300Service appMapService;

    @Test
    @Order(1)
    void capacitySeedShowsN1SoftWarning() throws Exception {
        ifina6200S0DTOin q = new ifina6200S0DTOin();
        q.setTargetTypeCd("GROUP");
        q.setTargetId("SG-WAS-A");
        var out = capacityService.ifina6200S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getRows()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(out.getWarnings().stream().anyMatch(w -> w.contains("RL-CP-001"))).isTrue();
    }

    @Test
    @Order(2)
    void capacityUpsertClearsN1WarningWhenRaised() throws Exception {
        ifina6200U0DTOin save = new ifina6200U0DTOin();
        save.setTargetTypeCd("GROUP");
        save.setTargetId("SG-WAS-A");
        save.setMetricScopeCd("N1");
        save.setTps(new BigDecimal("500"));
        save.setCpuPct(new BigDecimal("80"));
        save.setMemPct(new BigDecimal("75"));
        save.setRemark("N1 raised for test");
        var saved = capacityService.ifina6200U0(save);
        assertThat(saved.getRSLT_CD()).isEqualTo("0000");
        assertThat(saved.getWarnings().stream().noneMatch(w -> w.contains("RL-CP-001"))).isTrue();
    }

    @Test
    @Order(3)
    void appMapListCreateDelete() throws Exception {
        ifina2300S0DTOin q = new ifina2300S0DTOin();
        q.setAppId("APP-ONLINE-A");
        var list = appMapService.ifina2300S0(q);
        assertThat(list.getTotalCount()).isGreaterThanOrEqualTo(3);

        ifina2300C0DTOin create = new ifina2300C0DTOin();
        create.setMapId("AM-TEST-001");
        create.setAppId("APP-ONLINE-B");
        create.setMapTypeCd("SERVER");
        create.setRefId("INF-APP-001");
        create.setRoleCd("PRIMARY");
        assertThat(appMapService.ifina2300C0(create).getRSLT_CD()).isEqualTo("0000");

        ifina2300D0DTOin del = new ifina2300D0DTOin();
        del.setMapIdList(java.util.List.of("AM-TEST-001"));
        assertThat(appMapService.ifina2300D0(del).getRSLT_CD()).isEqualTo("0000");
    }
}
