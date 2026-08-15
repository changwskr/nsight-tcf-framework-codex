package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina0100S0DTOin;
import nhnis.infra.in.a.dto.ifina0100S0DTOout;
import nhnis.infra.in.a.dto.ifina1600S0DTOin;
import nhnis.infra.in.a.dto.ifina1600S0DTOout;
import nhnis.infra.in.a.dto.ifina3100C0DTOin;
import nhnis.infra.in.a.dto.ifina3100C0DTOout;
import nhnis.infra.in.a.application.support.InfraIntegrityValidator;
import nhnis.infra.in.a.application.support.ValidationResult;

@SpringBootTest
class ifinaAuditDashboardServiceTest {

    @Autowired private ifina1600Service auditService;
    @Autowired private ifina0100Service dashboardService;
    @Autowired private ifina3100Service assetService;
    @Autowired private InfraIntegrityValidator validator;

    @Test
    void changeLogSeedAndAssetCreateWritesLog() throws Exception {
        ifina1600S0DTOin seedQ = new ifina1600S0DTOin();
        seedQ.setEntityType("CHANGE");
        seedQ.setPageNo(1);
        seedQ.setPageSize(10);
        ifina1600S0DTOout seed = auditService.ifina1600S0(seedQ);
        assertThat(seed.getTotalCount()).isGreaterThanOrEqualTo(1);

        ifina3100C0DTOin create = new ifina3100C0DTOin();
        create.setAssetId("INF-AUD-3100");
        create.setAssetName("Audit Writer Asset");
        create.setAssetKindCd("VM");
        create.setEnvCd("PROD");
        create.setTechRoleCd("WAS");
        create.setSystemId("SYS-ONLINE");
        create.setGroupId("SG-WAS-A");
        ifina3100C0DTOout created = assetService.ifina3100C0(create);
        assertThat(created.getRSLT_CD()).isEqualTo("0000");

        ifina1600S0DTOin q = new ifina1600S0DTOin();
        q.setEntityType("CHANGE");
        q.setTargetId("INF-AUD-3100");
        q.setActionCd("CREATE");
        q.setPageNo(1);
        q.setPageSize(10);
        ifina1600S0DTOout logs = auditService.ifina1600S0(q);
        assertThat(logs.getTotalCount()).isGreaterThanOrEqualTo(1);
        assertThat(logs.getRows()).isNotEmpty();
    }

    @Test
    void evidenceSeedAndDashboardAggregate() throws Exception {
        ifina1600S0DTOin ev = new ifina1600S0DTOin();
        ev.setEntityType("EVIDENCE");
        ev.setPageNo(1);
        ev.setPageSize(10);
        assertThat(auditService.ifina1600S0(ev).getTotalCount()).isGreaterThanOrEqualTo(1);

        ifina0100S0DTOout dash = dashboardService.ifina0100S0(new ifina0100S0DTOin());
        assertThat(dash.getRSLT_CD()).isEqualTo("0000");
        assertThat(dash.getPilotAssetCount()).isGreaterThanOrEqualTo(1);
        assertThat(dash.getSystemCount()).isGreaterThanOrEqualTo(1);
        assertThat(dash.getChangeLogCount()).isGreaterThanOrEqualTo(1);
        assertThat(dash.getMwCount() + dash.getDbCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void validatorRejectsMissingAssetAndDupEndpoint() throws Exception {
        ValidationResult missing = validator.validateAssetCreate(
                null, "n", "VM", "PROD", "WAS", "SYS-ONLINE", "SG-WAS-A");
        assertThat(missing.hasHard()).isTrue();

        ValidationResult ep = validator.validateNetworkEndpoint(
                "EP-X", "INF-APP-001", "10.10.10.11", "7001", true);
        assertThat(ep.hasHard()).isTrue();
        assertThat(ep.firstHard().orElseThrow().getRsltCd()).isEqualTo("0005");
    }
}
