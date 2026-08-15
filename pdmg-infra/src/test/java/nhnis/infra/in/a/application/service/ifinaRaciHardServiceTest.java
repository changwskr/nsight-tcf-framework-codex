package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.infra.in.a.dto.ifina1100C0DTOin;
import nhnis.infra.in.a.dto.ifina2100C0DTOin;
import nhnis.infra.in.a.dto.ifina6300U0DTOin;
import nhnis.infra.in.a.dto.ifina7300C0DTOin;
import nhnis.infra.in.a.dto.ifina8100C0DTOin;
import nhnis.infra.in.a.dto.ifina9200U0DTOin;
import nhnis.infra.in.a.persistence.dao.ifinaAuditDAO;

@SpringBootTest
@TestPropertySource(properties = "infra.auth.raci.mode=hard")
class ifinaRaciHardServiceTest {

    @Autowired private ifina9200Service gateService;
    @Autowired private ifina7300Service costService;
    @Autowired private ifina8100Service planService;
    @Autowired private ifina2100Service systemService;
    @Autowired private ifina1100Service codeService;
    @Autowired private ifina6300Service securityService;
    @Autowired private ifinaAuditDAO auditDao;

    @AfterEach
    void clearCtx() {
        ServiceContextHolder.setInstance(null);
    }

    @Test
    void opsDeniedOnGateJudgeWith0006() throws Exception {
        bindOptr("E0000002");
        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE1");
        pass.setTargetTypeCd("ASSET");
        pass.setTargetId("INF-APP-001");
        pass.setResultCd("PASS");
        var out = gateService.ifina9200U0(pass);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");
        assertThat(out.getRSLT_MSG()).contains("RL-AU-003");
    }

    @Test
    void archAllowedPastRaciOnGate() throws Exception {
        bindOptr("E0000001");
        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE1");
        pass.setTargetTypeCd("ASSET");
        pass.setTargetId("INF-APP-001");
        pass.setResultCd("PASS");
        var out = gateService.ifina9200U0(pass);
        // RACI는 통과, Checklist HARD로 0001 가능
        assertThat(out.getRSLT_CD()).isNotEqualTo("0006");
    }

    @Test
    void opsDeniedOnCostWrite() throws Exception {
        bindOptr("E0000002");
        ifina7300C0DTOin in = new ifina7300C0DTOin();
        in.setCostId("COST-RACI-DENY");
        in.setCostTypeCd("CAPEX");
        in.setAmount(new BigDecimal("1"));
        var out = costService.ifina7300C0(in);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");
        assertThat(out.getRSLT_MSG()).contains("RL-AU-001");
    }

    @Test
    void opsDeniedOnMigrationPlanWrite() throws Exception {
        bindOptr("E0000002");
        ifina8100C0DTOin in = new ifina8100C0DTOin();
        in.setPlanId("PLAN-RACI-DENY");
        in.setTargetTypeCd("ASSET");
        in.setTargetId("INF-APP-001");
        in.setStrategy7rCd("REHOST");
        var out = planService.ifina8100C0(in);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");
        assertThat(out.getRSLT_MSG()).contains("RL-AU-001");
    }

    @Test
    void pmoDeniedOnInventoryWrite() throws Exception {
        bindOptr("E0000004");
        ifina2100C0DTOin in = new ifina2100C0DTOin();
        in.setSystemId("SYS-RACI-DENY");
        in.setSystemName("RACI deny");
        var out = systemService.ifina2100C0(in);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");
        assertThat(out.getRSLT_MSG()).contains("RL-AU-001");
    }

    @Test
    void opsDeniedOnMasterWriteAndAudited() throws Exception {
        bindOptr("E0000002");
        ifina1100C0DTOin in = new ifina1100C0DTOin();
        in.setCodeSetId("TECH_ROLE");
        in.setCodeValue("RACI-DENY");
        in.setNameKo("deny");
        var out = codeService.ifina1100C0(in);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");

        var logs = auditDao.ifina1600S0_S0(Map.of(
                "targetTypeCd", "RACI",
                "actionCd", "DENY",
                "keyword", "ifina1100C0",
                "offset", 0,
                "pageSize", 50));
        assertThat(logs).isNotEmpty();
        boolean foundOps = logs.stream().anyMatch(row -> {
            Object by = row.get("CHANGED_BY") != null ? row.get("CHANGED_BY") : row.get("changedBy");
            return "E0000002".equals(String.valueOf(by));
        });
        assertThat(foundOps).isTrue();
    }

    @Test
    void archDeniedOnMasterWrite() throws Exception {
        bindOptr("E0000001");
        ifina1100C0DTOin in = new ifina1100C0DTOin();
        in.setCodeSetId("TECH_ROLE");
        in.setCodeValue("RACI-ARCH-DENY");
        in.setNameKo("arch-deny");
        var out = codeService.ifina1100C0(in);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");
        assertThat(out.getRSLT_MSG()).contains("RL-AU-003");
    }

    @Test
    void adminAllowedOnMasterWrite() throws Exception {
        bindOptr("E0000005");
        ifina1100C0DTOin in = new ifina1100C0DTOin();
        in.setCodeSetId("TECH_ROLE");
        in.setCodeValue("RACI-ADMIN-OK");
        in.setNameKo("admin-ok");
        var out = codeService.ifina1100C0(in);
        assertThat(out.getRSLT_CD()).isNotEqualTo("0006");
    }

    @Test
    void secDeniedOnNonGate5() throws Exception {
        bindOptr("E0000003");
        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE1");
        pass.setTargetTypeCd("ASSET");
        pass.setTargetId("INF-APP-001");
        pass.setResultCd("PASS");
        var out = gateService.ifina9200U0(pass);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");
        assertThat(out.getRSLT_MSG()).contains("GATE5");
    }

    @Test
    void secAllowedPastRaciOnGate5() throws Exception {
        bindOptr("E0000003");
        ifina9200U0DTOin pass = new ifina9200U0DTOin();
        pass.setGateId("GATE5");
        pass.setTargetTypeCd("GROUP");
        pass.setTargetId("SG-WAS-A");
        pass.setResultCd("PASS");
        var out = gateService.ifina9200U0(pass);
        assertThat(out.getRSLT_CD()).isNotEqualTo("0006");
    }

    @Test
    void opsDeniedOnSecurityProfile() throws Exception {
        bindOptr("E0000002");
        ifina6300U0DTOin in = new ifina6300U0DTOin();
        in.setTargetTypeCd("GROUP");
        in.setTargetId("SG-WAS-A");
        in.setSecurityGradeCd("GENERAL");
        var out = securityService.ifina6300U0(in);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");
        assertThat(out.getRSLT_MSG()).contains("RL-AU-001");
    }

    private static void bindOptr(String optrEno) {
        sys_comm sys = new sys_comm();
        sys.setOptr_eno(optrEno);
        sys.setRms_svc_c("ifina9200U0");
        hdr_nhnis hdr = new hdr_nhnis();
        hdr.setSys_comm(sys);
        ServiceContext ctx = new ServiceContext("pdmg-infra", "TEST-GUID", "local", null, null, null, hdr);
        ServiceContextHolder.setInstance(ctx);
    }
}
