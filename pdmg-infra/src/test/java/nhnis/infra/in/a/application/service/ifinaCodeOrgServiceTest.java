package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina1100C0DTOin;
import nhnis.infra.in.a.dto.ifina1100S0DTOin;
import nhnis.infra.in.a.dto.ifina1100U0DTOin;
import nhnis.infra.in.a.dto.ifina1500C0DTOin;
import nhnis.infra.in.a.dto.ifina1500S0DTOin;
import nhnis.infra.in.a.dto.ifina1500U0DTOin;

@SpringBootTest
class ifinaCodeOrgServiceTest {

    @Autowired private ifina1100Service codeService;
    @Autowired private ifina1500Service orgService;

    @Test
    void codeSetSeedCreateAndDeactivate() throws Exception {
        ifina1100S0DTOin in = new ifina1100S0DTOin();
        in.setCodeSetId("SERVICE_MODEL");
        in.setPageNo(1);
        in.setPageSize(50);
        var list = codeService.ifina1100S0(in);
        assertThat(list.getCodeSets()).isNotEmpty();
        assertThat(list.getTotalCount()).isGreaterThanOrEqualTo(4);

        ifina1100C0DTOin create = new ifina1100C0DTOin();
        create.setCodeSetId("SERVICE_MODEL");
        create.setCodeValue("FAAS");
        create.setNameKo("FaaS");
        create.setSortOrder(9);
        assertThat(codeService.ifina1100C0(create).getRSLT_CD()).isEqualTo("0000");

        ifina1100U0DTOin off = new ifina1100U0DTOin();
        off.setCodeSetId("SERVICE_MODEL");
        off.setCodeValue("FAAS");
        off.setNameKo("FaaS");
        off.setSortOrder(9);
        off.setActiveYn("N");
        assertThat(codeService.ifina1100U0(off).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    void orgAndPersonFk() throws Exception {
        ifina1500S0DTOin orgIn = new ifina1500S0DTOin();
        orgIn.setEntityType("ORG");
        orgIn.setPageNo(1);
        orgIn.setPageSize(50);
        assertThat(orgService.ifina1500S0(orgIn).getTotalCount()).isGreaterThanOrEqualTo(5);

        ifina1500C0DTOin badPerson = new ifina1500C0DTOin();
        badPerson.setEntityType("PERSON");
        badPerson.setPersonId("P-BAD");
        badPerson.setPersonName("없음");
        badPerson.setOrgId("NO-ORG");
        assertThat(orgService.ifina1500C0(badPerson).getRSLT_CD()).isEqualTo("0004");

        ifina1500C0DTOin person = new ifina1500C0DTOin();
        person.setEntityType("PERSON");
        person.setPersonId("P-TEST-1500");
        person.setPersonName("테스트");
        person.setOrgId("ORG-INFRA");
        person.setEmail("test@example.com");
        assertThat(orgService.ifina1500C0(person).getRSLT_CD()).isEqualTo("0000");

        ifina1500U0DTOin off = new ifina1500U0DTOin();
        off.setEntityType("PERSON");
        off.setPersonId("P-TEST-1500");
        off.setPersonName("테스트");
        off.setOrgId("ORG-INFRA");
        off.setActiveYn("N");
        assertThat(orgService.ifina1500U0(off).getRSLT_CD()).isEqualTo("0000");
    }
}
