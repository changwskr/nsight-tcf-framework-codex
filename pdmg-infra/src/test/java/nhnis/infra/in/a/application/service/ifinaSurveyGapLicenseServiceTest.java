package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina7100C0DTOin;
import nhnis.infra.in.a.dto.ifina7100D0DTOin;
import nhnis.infra.in.a.dto.ifina7100S0DTOin;
import nhnis.infra.in.a.dto.ifina9300S0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaSurveyGapLicenseServiceTest {

    @Autowired private ifina9300Service gapService;
    @Autowired private ifina7100Service licenseService;

    @Test
    @Order(1)
    void surveyGapQueueAggregatesSources() throws Exception {
        var out = gapService.ifina9300S0(new ifina9300S0DTOin());
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getSize()).isGreaterThanOrEqualTo(3);
        assertThat(out.getChecklistCount() + out.getCapacityCount() + out.getWaveCount() + out.getStatusCount())
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(2)
    void licenseListShowsExpireSoftWarning() throws Exception {
        ifina7100S0DTOin q = new ifina7100S0DTOin();
        q.setPageNo(1);
        q.setPageSize(20);
        var out = licenseService.ifina7100S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getTotalCount()).isGreaterThanOrEqualTo(3);
        assertThat(out.getWarnings().stream().anyMatch(w -> w.contains("RL-LC-001"))).isTrue();
    }

    @Test
    @Order(3)
    void licenseDetailAllocations() throws Exception {
        ifina7100S0DTOin q = new ifina7100S0DTOin();
        q.setLicenseId("LIC-ORA-01");
        var out = licenseService.ifina7100S0(q);
        assertThat(out.getAllocations()).isNotEmpty();
    }

    @Test
    @Order(4)
    void licenseCreateDelete() throws Exception {
        ifina7100C0DTOin create = new ifina7100C0DTOin();
        create.setLicenseId("LIC-TEST-01");
        create.setProductName("Test Soft");
        create.setVendorName("Local");
        create.setLicenseModelCd("CORE");
        create.setQty(new BigDecimal("4"));
        create.setContractEndDt("2026-09-30");
        var saved = licenseService.ifina7100C0(create);
        assertThat(saved.getRSLT_CD()).isEqualTo("0000");
        assertThat(saved.getWarnings()).isNotEmpty();

        ifina7100D0DTOin del = new ifina7100D0DTOin();
        del.setLicenseIdList(List.of("LIC-TEST-01"));
        assertThat(licenseService.ifina7100D0(del).getRSLT_CD()).isEqualTo("0000");
    }
}
