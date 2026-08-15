package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina1999C0DTOin;
import nhnis.infra.in.a.dto.ifina1999C0DTOout;
import nhnis.infra.in.a.dto.ifina1999S0DTOin;
import nhnis.infra.in.a.dto.ifina1999S0DTOout;

@SpringBootTest
class ifina1999ServiceTest {

    @Autowired
    private ifina1999Service service;

    @Test
    void listSeedData() throws Exception {
        ifina1999S0DTOin in = new ifina1999S0DTOin();
        in.setPageNo(1);
        in.setPageSize(10);
        ifina1999S0DTOout out = service.ifina1999S0(in);
        assertThat(out.getTotalCount()).isGreaterThanOrEqualTo(3);
        assertThat(out.sizeifina1999S0DTOSub0()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void createAndFind() throws Exception {
        ifina1999C0DTOin create = new ifina1999C0DTOin();
        create.setServerId("INF-TEST-1999");
        create.setServerName("pilot-test");
        create.setTechRole("WAS");
        ifina1999C0DTOout created = service.ifina1999C0(create);
        assertThat(created.getRSLT_CD()).isEqualTo("0000");

        ifina1999S0DTOin in = new ifina1999S0DTOin();
        in.setServerId("INF-TEST-1999");
        in.setPageNo(1);
        in.setPageSize(10);
        ifina1999S0DTOout out = service.ifina1999S0(in);
        assertThat(out.getTotalCount()).isEqualTo(1);
    }
}
