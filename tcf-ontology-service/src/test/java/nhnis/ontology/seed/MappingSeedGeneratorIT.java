package nhnis.ontology.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.ontology.scan.InventorySnapshot;
import nhnis.ontology.scan.PdmgInventoryScanner;

@SpringBootTest
class MappingSeedGeneratorIT {

    @Autowired
    private PdmgInventoryScanner scanner;

    @Autowired
    private MappingSeedGenerator seedGenerator;

    @Test
    @EnabledIf("pdmgServiceExists")
    void seed_writesGeneratedAndSkipsCurated() throws Exception {
        InventorySnapshot inventory = scanner.scan();
        assertThat(inventory.getPrograms()).isNotEmpty();

        MappingSeedGenerator.SeedReport report = seedGenerator.generate(inventory, false);
        assertThat(report.getGenerated()).isNotEmpty();
        assertThat(report.getSkippedExisting()).contains("mgcoa9001.yml");

        Path draft = Path.of("ontology/mappings/_generated/mgcoa9001.yml");
        assertThat(Files.isRegularFile(draft)).isTrue();
        String yaml = Files.readString(draft);
        assertThat(yaml).contains("programId: mgcoa9001");
        assertThat(yaml).contains("TB_MG_TX_CONTROL");
        assertThat(yaml).contains("mgcoa9001S0");
    }

    static boolean pdmgServiceExists() {
        return Files.isDirectory(Path.of("..", "pdmg-service", "src", "main", "java"));
    }
}
