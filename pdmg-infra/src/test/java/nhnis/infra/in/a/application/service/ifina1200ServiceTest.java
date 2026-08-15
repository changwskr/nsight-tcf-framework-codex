package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina1200C0DTOin;
import nhnis.infra.in.a.dto.ifina1200D0DTOin;
import nhnis.infra.in.a.dto.ifina1200S0DTOin;
import nhnis.infra.in.a.dto.ifina1200U0DTOin;

@SpringBootTest
class ifina1200ServiceTest {

    @Autowired private ifina1200Service service;

    @Test
    void templateCatalogAndItemCrud() throws Exception {
        ifina1200S0DTOin in = new ifina1200S0DTOin();
        in.setEntityType("ITEM");
        in.setTemplateId("TMPL_WAS");
        in.setPageNo(1);
        in.setPageSize(50);
        var list = service.ifina1200S0(in);
        assertThat(list.getTemplates()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(list.getTotalCount()).isGreaterThanOrEqualTo(3);

        ifina1200C0DTOin create = new ifina1200C0DTOin();
        create.setEntityType("ITEM");
        create.setTemplateId("TMPL_WAS");
        create.setItemId("SI_TEST_1200");
        create.setItemName("테스트항목");
        create.setItemTypeCd("TEXT");
        create.setRequiredYn("N");
        create.setSortNo(99);
        assertThat(service.ifina1200C0(create).getRSLT_CD()).isEqualTo("0000");

        ifina1200U0DTOin upd = new ifina1200U0DTOin();
        upd.setEntityType("ITEM");
        upd.setTemplateId("TMPL_WAS");
        upd.setItemId("SI_TEST_1200");
        upd.setItemName("테스트항목2");
        upd.setItemTypeCd("TEXT");
        upd.setSortNo(98);
        assertThat(service.ifina1200U0(upd).getRSLT_CD()).isEqualTo("0000");

        ifina1200D0DTOin del = new ifina1200D0DTOin();
        del.setEntityType("ITEM");
        del.setTemplateId("TMPL_WAS");
        del.setItemIdList(List.of("SI_TEST_1200"));
        assertThat(service.ifina1200D0(del).getRSLT_CD()).isEqualTo("0000");
    }

    @Test
    void itemRequiresTemplateFk() throws Exception {
        ifina1200C0DTOin bad = new ifina1200C0DTOin();
        bad.setEntityType("ITEM");
        bad.setTemplateId("TMPL_NOPE");
        bad.setItemId("X");
        bad.setItemName("x");
        assertThat(service.ifina1200C0(bad).getRSLT_CD()).isEqualTo("0004");
    }
}
