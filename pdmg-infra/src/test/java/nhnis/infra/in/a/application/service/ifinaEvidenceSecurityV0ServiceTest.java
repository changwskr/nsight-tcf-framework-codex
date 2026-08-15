package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina1600C0DTOin;
import nhnis.infra.in.a.dto.ifina1600S0DTOin;
import nhnis.infra.in.a.dto.ifina6300S0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaEvidenceSecurityV0ServiceTest {

    @Autowired private ifina1600Service auditService;
    @Autowired private ifina6300Service securityService;

    @Test
    @Order(1)
    void evidenceRegisterAndList() throws Exception {
        ifina1600C0DTOin c = new ifina1600C0DTOin();
        c.setEvidenceId("EV-TEST-001");
        c.setTargetTypeCd("GROUP");
        c.setTargetId("SG-EV-PILOT");
        c.setGateId("GATE3");
        c.setFileName("gate3-minutes.pdf");
        c.setRemark("파일럿 증적");
        var created = auditService.ifina1600C0(c);
        assertThat(created.getRSLT_CD()).isEqualTo("0000");
        assertThat(created.getEvidenceId()).isEqualTo("EV-TEST-001");

        ifina1600S0DTOin q = new ifina1600S0DTOin();
        q.setEntityType("EVIDENCE");
        q.setTargetId("SG-EV-PILOT");
        q.setPageNo(1);
        q.setPageSize(20);
        var list = auditService.ifina1600S0(q);
        assertThat(list.getRows().stream().anyMatch(r -> "EV-TEST-001".equals(r.get("evidenceId")))).isTrue();
    }

    @Test
    @Order(2)
    void evidenceFileStoredFromBase64() throws Exception {
        ifina1600C0DTOin c = new ifina1600C0DTOin();
        c.setEvidenceId("EV-FILE-001");
        c.setTargetTypeCd("GROUP");
        c.setTargetId("SG-EV-FILE");
        c.setGateId("GATE3");
        c.setFileName("gate3-note.txt");
        c.setFileContentBase64(Base64.getEncoder().encodeToString("gate3 evidence body".getBytes(StandardCharsets.UTF_8)));
        c.setRemark("실파일 증적");
        var created = auditService.ifina1600C0(c);
        assertThat(created.getRSLT_CD()).isEqualTo("0000");
        assertThat(created.getRSLT_MSG()).isEqualTo("OK_FILE_STORED");
        assertThat(created.getFileUri()).isEqualTo("/evidence/EV-FILE-001/gate3-note.txt");
        assertThat(created.getSizeBytes()).isGreaterThan(0);
        assertThat(Files.isRegularFile(Path.of(System.getProperty("user.dir"), "data", "evidence", "EV-FILE-001", "gate3-note.txt"))).isTrue();
    }

    @Test
    @Order(3)
    void securityV0MissingProfileIsHard() throws Exception {
        ifina6300S0DTOin q = new ifina6300S0DTOin();
        q.setTargetTypeCd("GROUP");
        q.setTargetId("SG-BATCH-01");
        var v0 = securityService.ifina6300V0(q);
        assertThat(v0.getErrorCount()).isGreaterThan(0);
        assertThat(v0.getViolations().stream().anyMatch(v -> "RL-SC-001".equals(v.get("ruleId")))).isTrue();
    }

    @Test
    @Order(4)
    void securityV0SeedHasSoftWarnings() throws Exception {
        ifina6300S0DTOin q = new ifina6300S0DTOin();
        q.setTargetTypeCd("GROUP");
        q.setTargetId("SG-WAS-A");
        var v0 = securityService.ifina6300V0(q);
        assertThat(v0.getRSLT_CD()).isEqualTo("0000");
        assertThat(v0.getWarnCount()).isGreaterThan(0);
    }
}
