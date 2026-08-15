package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina5200C0DTOin;
import nhnis.infra.in.a.dto.ifina5200D0DTOin;
import nhnis.infra.in.a.dto.ifina5200S0DTOin;
import nhnis.infra.in.a.dto.ifina5200U0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifina5200ServiceTest {

    @Autowired private ifina5200Service service;

    @Test
    @Order(1)
    void listSeedInterfaces() throws Exception {
        ifina5200S0DTOin q = new ifina5200S0DTOin();
        q.setPageNo(1);
        q.setPageSize(20);
        var out = service.ifina5200S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getTotalCount()).isGreaterThanOrEqualTo(3);
        assertThat(out.getRows().stream().anyMatch(r -> "Y".equals(String.valueOf(r.get("criticalYn"))))).isTrue();
    }

    @Test
    @Order(2)
    void rejectMissingTargetAndUnknownApp() throws Exception {
        ifina5200C0DTOin noTarget = new ifina5200C0DTOin();
        noTarget.setInterfaceId("IF-BAD-01");
        noTarget.setFromAppId("APP-ONLINE-A");
        assertThat(service.ifina5200C0(noTarget).getRSLT_CD()).isEqualTo("0001");

        ifina5200C0DTOin badApp = new ifina5200C0DTOin();
        badApp.setInterfaceId("IF-BAD-02");
        badApp.setFromAppId("APP-MISSING");
        badApp.setToExternalName("X");
        assertThat(service.ifina5200C0(badApp).getRSLT_MSG()).contains("fromAppId");
    }

    @Test
    @Order(3)
    void createUpdateDelete() throws Exception {
        ifina5200C0DTOin create = new ifina5200C0DTOin();
        create.setInterfaceId("IF-TEST-01");
        create.setFromAppId("APP-ONLINE-A");
        create.setToAppId("APP-ONLINE-B");
        create.setProtocolCd("HTTPS");
        create.setDirectionCd("OUTBOUND");
        create.setCriticalYn("Y");
        assertThat(service.ifina5200C0(create).getRSLT_CD()).isEqualTo("0000");

        ifina5200U0DTOin upd = new ifina5200U0DTOin();
        upd.setInterfaceId("IF-TEST-01");
        upd.setFromAppId("APP-ONLINE-A");
        upd.setToExternalName("EXT-PARTNER");
        upd.setProtocolCd("MQ");
        upd.setDirectionCd("BIDIRECTIONAL");
        upd.setCriticalYn("N");
        assertThat(service.ifina5200U0(upd).getRSLT_CD()).isEqualTo("0000");

        ifina5200S0DTOin q = new ifina5200S0DTOin();
        q.setInterfaceId("IF-TEST-01");
        var found = service.ifina5200S0(q);
        assertThat(found.getRows()).hasSize(1);
        assertThat(found.getRows().get(0).get("toExternalName")).isEqualTo("EXT-PARTNER");

        ifina5200D0DTOin del = new ifina5200D0DTOin();
        del.setInterfaceIdList(List.of("IF-TEST-01"));
        assertThat(service.ifina5200D0(del).getRSLT_CD()).isEqualTo("0000");
    }
}
