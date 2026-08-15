package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina7200S0DTOin;
import nhnis.infra.in.a.dto.ifina7200U0DTOin;
import nhnis.infra.in.a.dto.ifina7300C0DTOin;
import nhnis.infra.in.a.dto.ifina7300S0DTOin;
import nhnis.infra.in.a.dto.ifina9400S0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaLicenseAllocTcoServiceTest {

    @Autowired private ifina7200Service allocService;
    @Autowired private ifina7300Service costService;
    @Autowired private ifina9400Service proposalService;

    @Test
    @Order(1)
    void allocQueryShowsRemaining() throws Exception {
        ifina7200S0DTOin q = new ifina7200S0DTOin();
        q.setLicenseId("LIC-ORA-01");
        var out = allocService.ifina7200S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getContractQty()).isNotNull();
        assertThat(out.getAllocatedSum()).isNotNull();
        assertThat(out.getRemainingQty()).isEqualByComparingTo(out.getContractQty().subtract(out.getAllocatedSum()));
        assertThat(out.getAllocations()).isNotEmpty();
    }

    @Test
    @Order(2)
    void allocHardRejectsOverQty() throws Exception {
        ifina7200U0DTOin u = new ifina7200U0DTOin();
        u.setLicenseId("LIC-MQ-01");
        u.setAllocations(List.of(
                Map.of("assetId", "INF-APP-001", "allocatedQty", new BigDecimal("9999"))
        ));
        var out = allocService.ifina7200U0(u);
        assertThat(out.getRSLT_CD()).isEqualTo("0004");
        assertThat(out.getRSLT_MSG()).contains("HARD");
    }

    @Test
    @Order(3)
    void allocReplaceWithinQty() throws Exception {
        ifina7200S0DTOin beforeQ = new ifina7200S0DTOin();
        beforeQ.setLicenseId("LIC-WL-01");
        var before = allocService.ifina7200S0(beforeQ);
        assertThat(before.getRSLT_CD()).isEqualTo("0000");

        ifina7200U0DTOin u = new ifina7200U0DTOin();
        u.setLicenseId("LIC-WL-01");
        u.setAllocations(List.of(
                Map.of("assetId", "INF-APP-001", "allocatedQty", new BigDecimal("4")),
                Map.of("assetId", "INF-APP-002", "allocatedQty", new BigDecimal("4"))
        ));
        var saved = allocService.ifina7200U0(u);
        assertThat(saved.getRSLT_CD()).isEqualTo("0000");

        var after = allocService.ifina7200S0(beforeQ);
        assertThat(after.getAllocatedSum()).isEqualByComparingTo("8");
        assertThat(after.getAllocations()).hasSize(2);

        // restore seed-like single allocation
        ifina7200U0DTOin restore = new ifina7200U0DTOin();
        restore.setLicenseId("LIC-WL-01");
        restore.setAllocations(List.of(Map.of("assetId", "INF-APP-001", "allocatedQty", new BigDecimal("8"))));
        assertThat(allocService.ifina7200U0(restore).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    @Order(4)
    void tcoSummaryFiveYear() throws Exception {
        ifina7300S0DTOin q = new ifina7300S0DTOin();
        q.setTargetId("SYS-ONLINE");
        q.setPeriodYm("202608");
        q.setYears(5);
        var out = costService.ifina7300S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getRows()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(out.getAsisAnnual()).isGreaterThan(BigDecimal.ZERO);
        assertThat(out.getTobeAnnual()).isGreaterThan(BigDecimal.ZERO);
        assertThat(out.getAsisTco()).isEqualByComparingTo(out.getAsisAnnual().multiply(BigDecimal.valueOf(5)));
        assertThat(out.getTobeTco()).isEqualByComparingTo(
                out.getTobeAnnual().multiply(BigDecimal.valueOf(5)).add(out.getMigrationOnce()));
        assertThat(out.getDeltaTco()).isEqualByComparingTo(out.getAsisTco().subtract(out.getTobeTco()));
    }

    @Test
    @Order(5)
    void tcoCreateAndProposalTable10() throws Exception {
        ifina7300C0DTOin c = new ifina7300C0DTOin();
        c.setCostId("CST-TEST-01");
        c.setTargetTypeCd("SYSTEM");
        c.setTargetId("SYS-ONLINE");
        c.setPeriodYm("202608");
        c.setScenarioCd("ASIS");
        c.setCostTypeCd("OPS");
        c.setAmount(new BigDecimal("1000"));
        c.setRemark("unit-test");
        assertThat(costService.ifina7300C0(c).getRSLT_CD()).isEqualTo("0000");

        ifina9400S0DTOin p = new ifina9400S0DTOin();
        p.setTableId(10);
        var prop = proposalService.ifina9400S0(p);
        assertThat(prop.getRSLT_CD()).isEqualTo("0000");
        assertThat(prop.getRSLT_MSG()).isEqualTo("OK");
        assertThat(prop.getRows()).isNotEmpty();
        assertThat(prop.getRows().get(0)).containsKeys("asisTco", "tobeTco", "deltaTco");
    }
}
