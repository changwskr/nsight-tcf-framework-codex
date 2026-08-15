package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina2200C0DTOin;
import nhnis.infra.in.a.dto.ifina2200S0DTOin;
import nhnis.infra.in.a.dto.ifina3400V0DTOin;
import nhnis.infra.in.a.dto.ifina3400V0DTOout;
import nhnis.infra.in.a.dto.ifina9100S0DTOin;
import nhnis.infra.in.a.dto.ifina9100S0DTOout;
import nhnis.infra.in.a.dto.ifina9100U0DTOin;

@SpringBootTest
class ifinaPhase1NextServiceTest {

    @Autowired private ifina2200Service appService;
    @Autowired private ifina3400Service bulkService;
    @Autowired private ifina9100Service checklistService;

    @Test
    void applicationSeedAndCreate() throws Exception {
        ifina2200S0DTOin in = new ifina2200S0DTOin();
        in.setPageNo(1);
        in.setPageSize(10);
        assertThat(appService.ifina2200S0(in).getTotalCount()).isGreaterThanOrEqualTo(3);

        ifina2200C0DTOin create = new ifina2200C0DTOin();
        create.setAppId("APP-TEST-2200");
        create.setAppName("테스트앱");
        create.setSystemId("SYS-ONLINE");
        assertThat(appService.ifina2200C0(create).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    void bulkValidateDetectsDuplicate() throws Exception {
        ifina3400V0DTOin in = new ifina3400V0DTOin();
        in.setRows(List.of(
                Map.of("serverId", "INF-NEW-3400", "serverName", "ok-server", "techRole", "WEB", "envCd", "DEV"),
                Map.of("serverId", "INF-APP-001", "serverName", "dup", "techRole", "WAS", "envCd", "PROD")
        ));
        ifina3400V0DTOout out = bulkService.ifina3400V0(in);
        assertThat(out.getOkCount()).isEqualTo(1);
        assertThat(out.getErrorCount()).isEqualTo(1);
        assertThat(out.getErrors().get(0).get("code")).isEqualTo("E_DUP_ID");
    }

    @Test
    void checklistProgress() throws Exception {
        ifina9100S0DTOin in = new ifina9100S0DTOin();
        in.setTargetType("ASSET");
        in.setTargetId("INF-APP-001");
        ifina9100S0DTOout out = checklistService.ifina9100S0(in);
        assertThat(out.getTotalItems()).isGreaterThanOrEqualTo(5);
        assertThat(out.getProgressPct()).isBetween(0, 100);

        ifina9100U0DTOin save = new ifina9100U0DTOin();
        save.setTargetType("ASSET");
        save.setTargetId("INF-APP-001");
        save.setItems(List.of(
                Map.of("checklistId", "CL-INV-03", "checkedYn", "Y", "remark", "연결완료")
        ));
        assertThat(checklistService.ifina9100U0(save).getRSLT_CD()).isEqualTo("0000");
    }
}
