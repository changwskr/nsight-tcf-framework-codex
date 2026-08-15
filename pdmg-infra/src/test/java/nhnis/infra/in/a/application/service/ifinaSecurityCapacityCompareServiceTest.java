package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina6300S0DTOin;
import nhnis.infra.in.a.dto.ifina6300U0DTOin;
import nhnis.infra.in.a.dto.ifina6400S0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaSecurityCapacityCompareServiceTest {

    @Autowired private ifina6300Service securityService;
    @Autowired private ifina6400Service compareService;

    @Test
    @Order(1)
    void securitySeedShowsSoftRisks() throws Exception {
        ifina6300S0DTOin q = new ifina6300S0DTOin();
        q.setTargetTypeCd("GROUP");
        q.setTargetId("SG-WAS-A");
        var out = securityService.ifina6300S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getWarnings().stream().anyMatch(w -> w.contains("RL-SC-002"))).isTrue();
    }

    @Test
    @Order(2)
    void securityUpsertClearsWarnings() throws Exception {
        ifina6300U0DTOin save = new ifina6300U0DTOin();
        save.setTargetTypeCd("GROUP");
        save.setTargetId("SG-WAS-A");
        save.setSecurityGradeCd("IMPORTANT");
        save.setPersonalInfoYn("Y");
        save.setAdminInfoYn("Y");
        save.setExternalConnYn("Y");
        save.setEncryptionYn("Y");
        save.setPamYn("Y");
        save.setAuditLogYn("Y");
        save.setNetworkZoneCd("APP");
        save.setAuthMethodCd("JWT");
        var saved = securityService.ifina6300U0(save);
        assertThat(saved.getRSLT_CD()).isEqualTo("0000");
        assertThat(saved.getWarnings()).isEmpty();
    }

    @Test
    @Order(3)
    void capacityComparePeakMatrix() throws Exception {
        ifina6400S0DTOin q = new ifina6400S0DTOin();
        q.setTargetTypeCd("GROUP");
        q.setMetricScopeCd("PEAK");
        q.setTargetIdList(List.of("SG-WAS-A", "SG-WAS-B"));
        var out = compareService.ifina6400S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getTargetIds()).containsExactly("SG-WAS-A", "SG-WAS-B");
        assertThat(out.getRows()).hasSize(2);
        assertThat(out.getMetrics()).hasSize(5);
        var tps = out.getMetrics().stream().filter(m -> "tps".equals(m.get("metricKey"))).findFirst();
        assertThat(tps).isPresent();
        assertThat(tps.get().get("maxTargetId")).isEqualTo("SG-WAS-A");
    }
}
