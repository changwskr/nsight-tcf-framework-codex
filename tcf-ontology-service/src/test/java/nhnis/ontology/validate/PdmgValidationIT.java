package nhnis.ontology.validate;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.ontology.scan.InventorySnapshot;
import nhnis.ontology.scan.PdmgInventoryScanner;

@SpringBootTest
class PdmgValidationIT {

    @Autowired
    private PdmgInventoryScanner scanner;

    @Autowired
    private OntologyValidator validator;

    @Test
    @EnabledIf("pdmgServiceExists")
    void scanAndValidate_pdmgPilot() throws Exception {
        InventorySnapshot inventory = scanner.scan();
        assertThat(inventory.getPrograms()).isNotEmpty();
        assertThat(inventory.getPrograms().stream().map(InventorySnapshot.ProgramInventory::getProgramId))
                .contains("mgcoa9001");

        Path yaml = scanner.writeYaml(inventory);
        assertThat(Files.isRegularFile(yaml)).isTrue();

        Map<String, Object> report = validator.validate(inventory);
        assertThat(report.get("status")).isIn("PASS", "PASS_WITH_WARNINGS", "FAIL");
        // 시드된 mgcoa9001 매핑과 소스는 일치해야 한다 (error 없이 warning만 허용 가능)
        assertThat(report.get("errorCount"))
                .as("validation report: %s", report)
                .isEqualTo(0);
    }

    static boolean pdmgServiceExists() {
        return Files.isDirectory(Path.of("..", "pdmg-service", "src", "main", "java"));
    }
}
