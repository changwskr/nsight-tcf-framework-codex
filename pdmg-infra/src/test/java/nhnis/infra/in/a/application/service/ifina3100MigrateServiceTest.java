package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina1999E0DTOin;
import nhnis.infra.in.a.dto.ifina1999E0DTOout;
import nhnis.infra.in.a.dto.ifina3100C0DTOin;
import nhnis.infra.in.a.dto.ifina3100S0DTOin;
import nhnis.infra.in.a.dto.ifina3100S1DTOin;

@SpringBootTest
class ifina3100MigrateServiceTest {

    @Autowired private ifina1999Service pilotService;
    @Autowired private ifina3100Service assetService;

    @Test
    void migratePilotThenQueryDetail() throws Exception {
        // seed에 정규 자산이 이미 있으면 skip, 없으면 migrate
        ifina1999E0DTOin e0 = new ifina1999E0DTOin();
        e0.setDefaultSystemId("SYS-ONLINE");
        ifina1999E0DTOout migrated = pilotService.ifina1999E0(e0);
        assertThat(migrated.getRSLT_CD()).isEqualTo("0000");
        assertThat(migrated.getMigratedCount() + migrated.getSkippedCount()).isGreaterThanOrEqualTo(3);

        ifina3100S0DTOin list = new ifina3100S0DTOin();
        list.setPageNo(1);
        list.setPageSize(20);
        assertThat(assetService.ifina3100S0(list).getTotalCount()).isGreaterThanOrEqualTo(3);

        ifina3100S1DTOin s1 = new ifina3100S1DTOin();
        s1.setAssetId("INF-APP-001");
        var detail = assetService.ifina3100S1(s1);
        assertThat(detail.getRSLT_CD()).isEqualTo("0000");
        assertThat(detail.getBase().getAssetId()).isEqualTo("INF-APP-001");
        assertThat(detail.getBase().getOsEolDate()).isEqualTo("2026-12-31");
        assertThat(detail.getEndpointCount()).isGreaterThanOrEqualTo(1);
        assertThat(detail.getMwCount()).isGreaterThanOrEqualTo(1);

        // skip on second run
        ifina1999E0DTOout again = pilotService.ifina1999E0(e0);
        assertThat(again.getSkippedCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void createRejectsInvalidCode() throws Exception {
        ifina3100C0DTOin bad = new ifina3100C0DTOin();
        bad.setAssetId("AST-BAD-CODE");
        bad.setAssetName("bad");
        bad.setAssetKindCd("VM");
        bad.setTechRoleCd("NO_SUCH_ROLE");
        bad.setEnvCd("PROD");
        assertThat(assetService.ifina3100C0(bad).getRSLT_CD()).isEqualTo("0004");
    }
}
