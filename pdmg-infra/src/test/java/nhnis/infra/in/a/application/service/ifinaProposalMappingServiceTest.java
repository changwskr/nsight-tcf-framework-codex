package nhnis.infra.in.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.infra.in.a.dto.ifina8300S0DTOin;
import nhnis.infra.in.a.dto.ifina9400E0DTOin;
import nhnis.infra.in.a.dto.ifina9400S0DTOin;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ifinaProposalMappingServiceTest {

    @Autowired private ifina8300Service mappingService;
    @Autowired private ifina9400Service proposalService;

    @Test
    @Order(1)
    void asisTobeMappingHasGapHint() throws Exception {
        ifina8300S0DTOin q = new ifina8300S0DTOin();
        q.setPageNo(1);
        q.setPageSize(50);
        var out = mappingService.ifina8300S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getTotalCount()).isGreaterThanOrEqualTo(5);
        assertThat(out.getRows().stream().anyMatch(r -> r.get("gapHint") != null
                && !String.valueOf(r.get("gapHint")).isBlank())).isTrue();
    }

    @Test
    @Order(2)
    void proposalTable9Mapping() throws Exception {
        ifina9400S0DTOin q = new ifina9400S0DTOin();
        q.setTableId(9);
        var out = proposalService.ifina9400S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getTableName()).contains("전환");
        assertThat(out.getRows()).isNotEmpty();
        assertThat(out.getColumns()).isNotEmpty();
    }

    @Test
    @Order(3)
    void proposalTable2CapacityAndInventory() throws Exception {
        ifina9400S0DTOin t2 = new ifina9400S0DTOin();
        t2.setTableId(2);
        var cap = proposalService.ifina9400S0(t2);
        assertThat(cap.getRSLT_CD()).isEqualTo("0000");
        assertThat(cap.getRows()).isNotEmpty();

        ifina9400S0DTOin inv = new ifina9400S0DTOin();
        inv.setTableId(0);
        var inventory = proposalService.ifina9400S0(inv);
        assertThat(inventory.getRSLT_CD()).isEqualTo("0000");
        assertThat(inventory.getRows()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @Order(4)
    void proposalTable10Tco() throws Exception {
        ifina9400S0DTOin q = new ifina9400S0DTOin();
        q.setTableId(10);
        var out = proposalService.ifina9400S0(q);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getRSLT_MSG()).isEqualTo("OK");
        assertThat(out.getTableName()).contains("TCO");
        assertThat(out.getRows()).isNotEmpty();
        assertThat(out.getRows().get(0)).containsKeys("asisTco", "tobeTco", "deltaTco");
        assertThat(out.getRows().get(0).get("targetId")).isEqualTo("SYS-ONLINE");
        assertThat(out.getRows().get(0)).containsKeys("asisHw", "tobeCloud", "years");
    }

    @Test
    @Order(5)
    void proposalExportCsv() throws Exception {
        ifina9400E0DTOin e = new ifina9400E0DTOin();
        e.setTableId(9);
        e.setFormatCd("CSV");
        var out = proposalService.ifina9400E0(e);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getDownloadUri()).startsWith("/exports/");
        assertThat(out.getFileName()).endsWith(".csv");
        assertThat(out.getRowCount()).isGreaterThan(0);

        ifina9400E0DTOin pdf = new ifina9400E0DTOin();
        pdf.setTableId(9);
        pdf.setFormatCd("PDF");
        var pdfOut = proposalService.ifina9400E0(pdf);
        assertThat(pdfOut.getRSLT_CD()).isEqualTo("0000");
        assertThat(pdfOut.getFileName()).endsWith(".pdf");
        assertThat(pdfOut.getDownloadUri()).startsWith("/exports/");
    }

    @Test
    @Order(6)
    void proposalExportXlsxMultiSevenPlus() throws Exception {
        ifina9400E0DTOin e = new ifina9400E0DTOin();
        e.setAllTablesYn("Y");
        e.setFormatCd("XLSX");
        var out = proposalService.ifina9400E0(e);
        assertThat(out.getRSLT_CD()).isEqualTo("0000");
        assertThat(out.getFormatCd()).isEqualTo("XLSX");
        assertThat(out.getFileName()).endsWith(".xlsx");
        assertThat(out.getTableCount()).isGreaterThanOrEqualTo(7);
        assertThat(out.getExportedTableIds()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(out.getDownloadUri()).startsWith("/exports/");
    }
}
