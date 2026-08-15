package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina2100C0DTOin;
import nhnis.infra.in.a.dto.ifina2100D0DTOin;
import nhnis.infra.in.a.dto.ifina2100S0DTOin;
import nhnis.infra.in.a.dto.ifina2100S0DTOout;
import nhnis.infra.in.a.dto.ifina3110C0DTOin;
import nhnis.infra.in.a.dto.ifina3110S0DTOin;
import nhnis.infra.in.a.dto.ifina3110S0DTOout;

@SpringBootTest
class ifina2100ServiceTest {

    @Autowired
    private ifina2100Service systemService;
    @Autowired
    private ifina3110Service groupService;

    @Test
    void listSeedSystemsAndGroups() throws Exception {
        ifina2100S0DTOin sysIn = new ifina2100S0DTOin();
        sysIn.setPageNo(1);
        sysIn.setPageSize(10);
        ifina2100S0DTOout systems = systemService.ifina2100S0(sysIn);
        assertThat(systems.getTotalCount()).isGreaterThanOrEqualTo(3);

        ifina3110S0DTOin grpIn = new ifina3110S0DTOin();
        grpIn.setPageNo(1);
        grpIn.setPageSize(10);
        ifina3110S0DTOout groups = groupService.ifina3110S0(grpIn);
        assertThat(groups.getTotalCount()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void createSystemAndRejectDeleteWhenGroupLinked() throws Exception {
        ifina2100C0DTOin create = new ifina2100C0DTOin();
        create.setSystemId("SYS-TEST-2100");
        create.setSystemName("테스트시스템");
        assertThat(systemService.ifina2100C0(create).getRSLT_CD()).isEqualTo("0000");

        ifina3110C0DTOin group = new ifina3110C0DTOin();
        group.setGroupId("SG-TEST-2100");
        group.setGroupName("테스트군");
        group.setSystemId("SYS-TEST-2100");
        group.setTechRoleCd("WAS");
        group.setEnvCd("DEV");
        assertThat(groupService.ifina3110C0(group).getRSLT_CD()).isEqualTo("0000");

        ifina2100D0DTOin del = new ifina2100D0DTOin();
        del.setSystemIdList(java.util.List.of("SYS-TEST-2100"));
        assertThat(systemService.ifina2100D0(del).getRSLT_CD()).isEqualTo("0005");
    }
}
