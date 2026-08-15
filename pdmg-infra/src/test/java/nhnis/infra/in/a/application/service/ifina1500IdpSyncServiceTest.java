package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina1500E0DTOin;
import nhnis.infra.in.a.dto.ifina1500E0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina1500DAO;

@SpringBootTest
class ifina1500IdpSyncServiceTest {

    @Autowired private ifina1500Service personService;
    @Autowired private ifina1500DAO personDao;

    @Test
    void syncUpdatesExistingAndCreatesMissing() throws Exception {
        ifina1500E0DTOin in = new ifina1500E0DTOin();
        in.setEntries(List.of(
                Map.of("personId", "E0000002", "idpRole", "dba"),
                Map.of(
                        "personId", "IDP-NEW-001",
                        "idpRole", "infra-ops",
                        "personName", "IdP신규",
                        "email", "idp.new@example.com"),
                Map.of("personId", "E0000001", "idpRole", "no-such-group")));

        ifina1500E0DTOout out = personService.ifina1500E0(in);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getSyncedCount()).isGreaterThanOrEqualTo(1);
        assertThat(out.getCreatedCount()).isGreaterThanOrEqualTo(1);
        assertThat(out.getErrorCount()).isGreaterThanOrEqualTo(1);
        assertThat(personDao.ifina1500S0_roleByPersonId(Map.of("personId", "E0000002"))).isEqualTo("DBA");
        assertThat(personDao.ifina1500S0_person_exists(Map.of("personId", "IDP-NEW-001"))).isEqualTo(1);
        assertThat(personDao.ifina1500S0_roleByPersonId(Map.of("personId", "IDP-NEW-001"))).isEqualTo("OPS");
    }

    @Test
    void dryRunDoesNotPersist() throws Exception {
        ifina1500E0DTOin in = new ifina1500E0DTOin();
        in.setDryRunYn("Y");
        in.setEntries(List.of(Map.of("personId", "E0000003", "idpRole", "pmo")));
        String before = personDao.ifina1500S0_roleByPersonId(Map.of("personId", "E0000003"));
        ifina1500E0DTOout out = personService.ifina1500E0(in);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getRSLT_MSG()).isEqualTo("DRY_OK");
        assertThat(personDao.ifina1500S0_roleByPersonId(Map.of("personId", "E0000003"))).isEqualTo(before);
    }
}
