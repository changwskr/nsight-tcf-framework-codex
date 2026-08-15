package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina3100C0DTOin;
import nhnis.infra.in.a.dto.ifina3100S1DTOin;
import nhnis.infra.in.a.dto.ifina3100U0DTOin;

@SpringBootTest
class ifina3100AttrServiceTest {

    @Autowired private ifina3100Service assetService;

    @Test
    void attrsLoadByTechRoleAndUpsert() throws Exception {
        ifina3100C0DTOin create = new ifina3100C0DTOin();
        create.setAssetId("AST-EAV-01");
        create.setAssetName("eav-was");
        create.setAssetKindCd("VM");
        create.setTechRoleCd("WAS");
        create.setEnvCd("DEV");
        create.setStatusCd("DISCOVERED");
        create.setAttrs(List.of(
                Map.of("itemId", "SI_JVM", "attrValue", "8"),
                Map.of("itemId", "SI_OWNER", "attrValue", "정보계운영")));
        assertThat(assetService.ifina3100C0(create).getRSLT_CD()).isEqualTo("0000");

        ifina3100S1DTOin s1 = new ifina3100S1DTOin();
        s1.setAssetId("AST-EAV-01");
        var detail = assetService.ifina3100S1(s1);
        assertThat(detail.getRSLT_CD()).isEqualTo("0000");
        assertThat(detail.getAttrCount()).isGreaterThanOrEqualTo(5); // COMMON+WAS
        assertThat(detail.getAttrs().stream()
                .anyMatch(a -> "SI_JVM".equals(a.get("itemId")) && "8".equals(String.valueOf(a.get("attrValue")))))
                .isTrue();
        assertThat(detail.getWarnings().stream().anyMatch(w -> w.contains("SI_OPS"))).isTrue();

        ifina3100U0DTOin upd = new ifina3100U0DTOin();
        upd.setAssetId("AST-EAV-01");
        upd.setAssetName("eav-was");
        upd.setAssetKindCd("VM");
        upd.setTechRoleCd("WAS");
        upd.setEnvCd("DEV");
        upd.setStatusCd("DISCOVERED");
        upd.setAttrs(List.of(
                Map.of("itemId", "SI_JVM", "attrValue", "16"),
                Map.of("itemId", "SI_OWNER", "attrValue", "정보계운영"),
                Map.of("itemId", "SI_OPS", "attrValue", "WAS운영")));
        var out = assetService.ifina3100U0(upd);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getRSLT_MSG()).doesNotContain("SI_OPS");

        var after = assetService.ifina3100S1(s1);
        assertThat(after.getAttrs().stream()
                .anyMatch(a -> "SI_JVM".equals(a.get("itemId")) && "16".equals(String.valueOf(a.get("attrValue")))))
                .isTrue();
    }
}
