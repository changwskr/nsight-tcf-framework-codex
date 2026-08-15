package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina4100C0DTOin;
import nhnis.infra.in.a.dto.ifina4100S0DTOin;
import nhnis.infra.in.a.dto.ifina4200C0DTOin;
import nhnis.infra.in.a.dto.ifina4200S0DTOin;
import nhnis.infra.in.a.dto.ifina5100C0DTOin;
import nhnis.infra.in.a.dto.ifina5100S0DTOin;

@SpringBootTest
class ifinaMwDbNetworkServiceTest {

    @Autowired private ifina4100Service mwService;
    @Autowired private ifina4200Service dbService;
    @Autowired private ifina5100Service networkService;

    @Test
    void middlewareSeedAndCreate() throws Exception {
        ifina4100S0DTOin in = new ifina4100S0DTOin();
        in.setPageNo(1);
        in.setPageSize(10);
        assertThat(mwService.ifina4100S0(in).getTotalCount()).isGreaterThanOrEqualTo(2);

        ifina4100C0DTOin create = new ifina4100C0DTOin();
        create.setMwId("MW-TEST-4100");
        create.setAssetId("INF-APP-001");
        create.setProductName("Tomcat");
        create.setVersionNo("10.1");
        assertThat(mwService.ifina4100C0(create).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    void dbSeedAndFkValidation() throws Exception {
        ifina4200S0DTOin in = new ifina4200S0DTOin();
        in.setPageNo(1);
        in.setPageSize(10);
        assertThat(dbService.ifina4200S0(in).getTotalCount()).isGreaterThanOrEqualTo(1);

        ifina4200C0DTOin bad = new ifina4200C0DTOin();
        bad.setDbId("DB-BAD-4200");
        bad.setDbName("bad");
        bad.setAssetId("NO-SUCH-ASSET");
        assertThat(dbService.ifina4200C0(bad).getRSLT_CD()).isEqualTo("0004");

        ifina4200C0DTOin ok = new ifina4200C0DTOin();
        ok.setDbId("DB-TEST-4200");
        ok.setDbName("TESTDB");
        ok.setAssetId("INF-DB-001");
        ok.setSystemId("SYS-ONLINE");
        assertThat(dbService.ifina4200C0(ok).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    void networkDupAddressAndPrimary() throws Exception {
        ifina5100S0DTOin in = new ifina5100S0DTOin();
        in.setPageNo(1);
        in.setPageSize(10);
        assertThat(networkService.ifina5100S0(in).getTotalCount()).isGreaterThanOrEqualTo(3);

        ifina5100C0DTOin dup = new ifina5100C0DTOin();
        dup.setEndpointId("EP-DUP-5100");
        dup.setAssetId("INF-APP-001");
        dup.setAddress("10.10.10.11");
        dup.setPortNo("7001");
        assertThat(networkService.ifina5100C0(dup).getRSLT_CD()).isEqualTo("0005");

        ifina5100C0DTOin primary = new ifina5100C0DTOin();
        primary.setEndpointId("EP-PRI-5100");
        primary.setAssetId("INF-APP-001");
        primary.setAddress("10.10.10.199");
        primary.setPortNo("7999");
        primary.setPrimaryYn("Y");
        assertThat(networkService.ifina5100C0(primary).getRSLT_CD()).isEqualTo("0000");

        ifina5100S0DTOin q = new ifina5100S0DTOin();
        q.setAssetId("INF-APP-001");
        q.setPrimaryYn("Y");
        q.setPageNo(1);
        q.setPageSize(20);
        assertThat(networkService.ifina5100S0(q).getTotalCount()).isEqualTo(1);
    }
}
