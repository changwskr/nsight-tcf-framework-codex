package nhnis.ontology.job;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import nhnis.ontology.config.OntologyProperties;
import nhnis.ontology.prompt.PromptContextExporter;
import nhnis.ontology.scan.InventorySnapshot;
import nhnis.ontology.scan.PdmgInventoryScanner;
import nhnis.ontology.seed.MappingSeedGenerator;
import nhnis.ontology.validate.OntologyValidator;

@Slf4j
@Component
@ConditionalOnProperty(name = "nhnis.ontology.job")
public class OntologyJobRunner implements ApplicationRunner {

    private final String job;
    private final String targetId;
    private final boolean seedOverwrite;
    private final PdmgInventoryScanner scanner;
    private final OntologyValidator validator;
    private final PromptContextExporter promptExporter;
    private final MappingSeedGenerator seedGenerator;
    private final OntologyProperties properties;
    private final ObjectMapper objectMapper;
    private final ConfigurableApplicationContext context;

    public OntologyJobRunner(
            @Value("${nhnis.ontology.job}") String job,
            @Value("${nhnis.ontology.target:mgcoa9001}") String targetId,
            @Value("${nhnis.ontology.seed.overwrite:false}") boolean seedOverwrite,
            PdmgInventoryScanner scanner,
            OntologyValidator validator,
            PromptContextExporter promptExporter,
            MappingSeedGenerator seedGenerator,
            OntologyProperties properties,
            ObjectMapper objectMapper,
            ConfigurableApplicationContext context) {
        this.job = job;
        this.targetId = targetId;
        this.seedOverwrite = seedOverwrite;
        this.scanner = scanner;
        this.validator = validator;
        this.promptExporter = promptExporter;
        this.seedGenerator = seedGenerator;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int exitCode = 0;
        switch (job) {
            case "import" -> {
                InventorySnapshot snapshot = scanner.scan();
                Path out = scanner.writeYaml(snapshot);
                log.info("import done: programs={}, output={}", snapshot.getPrograms().size(), out);
            }
            case "seed" -> {
                InventorySnapshot snapshot = scanner.scan();
                scanner.writeYaml(snapshot);
                MappingSeedGenerator.SeedReport report = seedGenerator.generate(snapshot, seedOverwrite);
                Path out = Path.of(System.getProperty("user.dir"))
                        .resolve("test-data/queries/last-seed-report.json")
                        .normalize();
                Files.createDirectories(out.getParent());
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(out.toFile(), report.toMap());
                log.info("seed report: {}", out);
            }
            case "validate" -> {
                Map<String, Object> report = validator.scanAndValidate();
                Path out = Path.of(System.getProperty("user.dir"))
                        .resolve(properties.getScan().getReportOutput())
                        .normalize();
                Files.createDirectories(out.getParent());
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
                Files.writeString(out, json, StandardCharsets.UTF_8);
                log.info("validate status={} errors={} warnings={} report={}",
                        report.get("status"), report.get("errorCount"), report.get("warningCount"), out);
                if ("FAIL".equals(report.get("status"))) {
                    exitCode = 2;
                }
            }
            case "prompt" -> {
                String markdown = promptExporter.asMarkdown(targetId);
                Path out = Path.of(System.getProperty("user.dir"))
                        .resolve("test-data/queries/prompt-context-" + targetId + ".md")
                        .normalize();
                Files.createDirectories(out.getParent());
                Files.writeString(out, markdown, StandardCharsets.UTF_8);
                log.info("prompt context written: {}", out);
            }
            default -> {
                log.error("Unknown nhnis.ontology.job={}", job);
                exitCode = 1;
            }
        }
        final int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
