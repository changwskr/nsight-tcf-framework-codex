package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.infra.in.a.dto.ifina3100C0DTOin;
import nhnis.infra.in.a.dto.ifina3100U0DTOin;

@SpringBootTest
class ifina3100LifecycleServiceTest {

    @Autowired private ifina3100Service assetService;

    @AfterEach
    void clear() {
        ServiceContextHolder.setInstance(null);
    }

    @Test
    void opsBlockedOnLifecycleJump() throws Exception {
        ifina3100C0DTOin create = new ifina3100C0DTOin();
        create.setAssetId("AST-LF-JUMP");
        create.setAssetName("lifecycle-jump");
        create.setAssetKindCd("VM");
        create.setTechRoleCd("WAS");
        create.setEnvCd("DEV");
        create.setStatusCd("DISCOVERED");
        assertThat(assetService.ifina3100C0(create).getRSLT_CD()).isEqualTo("0000");

        bind("E0000002");
        ifina3100U0DTOin jump = new ifina3100U0DTOin();
        jump.setAssetId("AST-LF-JUMP");
        jump.setAssetName("lifecycle-jump");
        jump.setAssetKindCd("VM");
        jump.setTechRoleCd("WAS");
        jump.setEnvCd("DEV");
        jump.setStatusCd("CONFIRMED");
        var out = assetService.ifina3100U0(jump);
        assertThat(out.getRSLT_CD()).isEqualTo("0006");
        assertThat(out.getRSLT_MSG()).contains("RL-AU-002");
    }

    @Test
    void reverseLifecycleBlocked() throws Exception {
        ifina3100C0DTOin create = new ifina3100C0DTOin();
        create.setAssetId("AST-LF-REV");
        create.setAssetName("lifecycle-rev");
        create.setAssetKindCd("VM");
        create.setTechRoleCd("WAS");
        create.setEnvCd("DEV");
        create.setStatusCd("CONFIRMED");
        assertThat(assetService.ifina3100C0(create).getRSLT_CD()).isEqualTo("0000");

        ifina3100U0DTOin rev = new ifina3100U0DTOin();
        rev.setAssetId("AST-LF-REV");
        rev.setAssetName("lifecycle-rev");
        rev.setAssetKindCd("VM");
        rev.setTechRoleCd("WAS");
        rev.setEnvCd("DEV");
        rev.setStatusCd("DISCOVERED");
        var out = assetService.ifina3100U0(rev);
        assertThat(out.getRSLT_CD()).isEqualTo("0005");
        assertThat(out.getRSLT_MSG()).contains("RL-LF-002");
    }

    @Test
    void archMayJumpLifecycle() throws Exception {
        ifina3100C0DTOin create = new ifina3100C0DTOin();
        create.setAssetId("AST-LF-ARCH");
        create.setAssetName("lifecycle-arch");
        create.setAssetKindCd("VM");
        create.setTechRoleCd("WAS");
        create.setEnvCd("DEV");
        create.setStatusCd("DISCOVERED");
        assertThat(assetService.ifina3100C0(create).getRSLT_CD()).isEqualTo("0000");

        bind("E0000001");
        ifina3100U0DTOin jump = new ifina3100U0DTOin();
        jump.setAssetId("AST-LF-ARCH");
        jump.setAssetName("lifecycle-arch");
        jump.setAssetKindCd("VM");
        jump.setTechRoleCd("WAS");
        jump.setEnvCd("DEV");
        jump.setStatusCd("TARGET_DEFINED");
        var out = assetService.ifina3100U0(jump);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
    }

    private static void bind(String optr) {
        sys_comm sys = new sys_comm();
        sys.setOptr_eno(optr);
        hdr_nhnis hdr = new hdr_nhnis();
        hdr.setSys_comm(sys);
        ServiceContextHolder.setInstance(
                new ServiceContext("pdmg-infra", "GUID", "local", null, null, null, hdr));
    }
}
