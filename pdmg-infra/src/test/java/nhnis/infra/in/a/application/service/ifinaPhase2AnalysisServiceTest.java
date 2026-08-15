package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina0200S0DTOin;
import nhnis.infra.in.a.dto.ifina0200S0DTOout;
import nhnis.infra.in.a.dto.ifina4300S0DTOin;
import nhnis.infra.in.a.dto.ifina4300S0DTOout;
import nhnis.infra.in.a.dto.ifina5300C0DTOin;
import nhnis.infra.in.a.dto.ifina5300S0DTOin;
import nhnis.infra.in.a.dto.ifina5300S0DTOout;

@SpringBootTest
class ifinaPhase2AnalysisServiceTest {

    @Autowired private ifina5300Service relationService;
    @Autowired private ifina4300Service eolService;
    @Autowired private ifina0200Service riskService;

    @Test
    void dependencyMapBfsAndCreate() throws Exception {
        ifina5300S0DTOin q = new ifina5300S0DTOin();
        q.setRootType("ASSET");
        q.setRootId("INF-APP-001");
        q.setDepth(2);
        ifina5300S0DTOout graph = relationService.ifina5300S0(q);
        assertThat(graph.getEdges()).isNotEmpty();
        assertThat(graph.getNodes()).isNotEmpty();
        assertThat(graph.getEdges().stream().anyMatch(e -> "USES_DB".equals(e.get("relationTypeCd")))).isTrue();

        ifina5300C0DTOin create = new ifina5300C0DTOin();
        create.setRelationId("REL-TEST-5300");
        create.setFromTypeCd("APP");
        create.setFromId("APP-BATCH-01");
        create.setToTypeCd("ASSET");
        create.setToId("INF-APP-001");
        create.setRelationTypeCd("CALLS");
        create.setCriticalYn("N");
        assertThat(relationService.ifina5300C0(create).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    void eolViewWithin12Months() throws Exception {
        ifina4300S0DTOin in = new ifina4300S0DTOin();
        in.setMaxDaysLeft(365);
        in.setPageNo(1);
        in.setPageSize(50);
        ifina4300S0DTOout out = eolService.ifina4300S0(in);
        assertThat(out.getTotalCount()).isGreaterThanOrEqualTo(2);
        assertThat(out.getRows()).anyMatch(r -> "MW-MQ-01".equals(r.get("objectId")));
        assertThat(out.getRows()).anyMatch(r -> "OS".equals(r.get("sourceCd")) && "INF-APP-001".equals(r.get("objectId")));
    }

    @Test
    void eolOsSourceFilter() throws Exception {
        ifina4300S0DTOin in = new ifina4300S0DTOin();
        in.setSourceCd("OS");
        in.setMaxDaysLeft(800);
        in.setPageNo(1);
        in.setPageSize(20);
        ifina4300S0DTOout out = eolService.ifina4300S0(in);
        assertThat(out.getRows()).isNotEmpty();
        assertThat(out.getRows()).allMatch(r -> "OS".equals(r.get("sourceCd")));
    }

    @Test
    void riskWorklistAggregatesSources() throws Exception {
        ifina0200S0DTOout all = riskService.ifina0200S0(new ifina0200S0DTOin());
        assertThat(all.getRSLT_CD()).isEqualTo("0000");
        assertThat(all.getEolCount()).isGreaterThanOrEqualTo(1);
        assertThat(all.getGateOpenCount() + all.getChecklistOpenCount()).isGreaterThanOrEqualTo(1);
        assertThat(all.getRows()).isNotEmpty();
        assertThat(all.getRows().stream().anyMatch(r ->
                "EOL".equals(r.get("riskType")) && "OS".equals(String.valueOf(r.get("targetTypeCd")))))
                .as("INF-020 EOL should include OS via V_IF_EOL_RISK")
                .isTrue();

        ifina0200S0DTOin gateOnly = new ifina0200S0DTOin();
        gateOnly.setRiskType("GATE");
        assertThat(riskService.ifina0200S0(gateOnly).getRows())
                .allMatch(r -> "GATE".equals(r.get("riskType")));
    }
}
