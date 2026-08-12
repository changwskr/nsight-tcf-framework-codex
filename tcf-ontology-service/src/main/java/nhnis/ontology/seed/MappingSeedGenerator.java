package nhnis.ontology.seed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nhnis.ontology.config.OntologyProperties;
import nhnis.ontology.scan.InventorySnapshot;

@Slf4j
@Service
public class MappingSeedGenerator {

    private static final Pattern TABLE_REF = Pattern.compile(
            "(?:FROM|INTO|UPDATE|DELETE\\s+FROM)\\s+(TB_[A-Z0-9_]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PK_HINT = Pattern.compile(
            "CONSTRAINT\\s+\\w+_PK\\s+PRIMARY\\s+KEY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE);

    private final OntologyProperties properties;

    public MappingSeedGenerator(OntologyProperties properties) {
        this.properties = properties;
    }

    public SeedReport generate(InventorySnapshot inventory, boolean overwriteExisting) throws IOException {
        Path mappingsDir = Path.of(System.getProperty("user.dir")).resolve("ontology/mappings").normalize();
        Path generatedDir = mappingsDir.resolve("_generated");
        Files.createDirectories(generatedDir);

        Path serviceRoot = resolve(properties.getScan().getPdmgService());
        Path resources = serviceRoot.resolve("src/main/resources");
        Path javaRoot = serviceRoot.resolve("src/main/java");
        Path schema = resources.resolve("db/h2/schema.sql");
        Path samplesDir = resolve(properties.getScan().getPdmgUi())
                .resolve("src/main/resources/sample-requests");

        String schemaText = Files.isRegularFile(schema)
                ? Files.readString(schema, StandardCharsets.UTF_8)
                : "";

        SeedReport report = new SeedReport();
        for (InventorySnapshot.ProgramInventory program : inventory.getPrograms()) {
            Map<String, Object> doc = buildDocument(program, resources, javaRoot, samplesDir, schemaText);
            String fileName = program.getProgramId() + ".yml";

            Path generated = generatedDir.resolve(fileName);
            writeYaml(generated, doc);
            report.getGenerated().add(relativize(generated));

            Path curated = mappingsDir.resolve(fileName);
            if (Files.exists(curated) && !overwriteExisting) {
                report.getSkippedExisting().add(fileName);
            } else {
                boolean existed = Files.exists(curated);
                writeYaml(curated, doc);
                if (existed) {
                    report.getOverwritten().add(fileName);
                } else {
                    report.getCreated().add(fileName);
                }
            }
        }
        report.setProgramCount(inventory.getPrograms().size());
        log.info("seed done: created={}, skipped={}, overwritten={}, generatedDir={}",
                report.getCreated().size(), report.getSkippedExisting().size(),
                report.getOverwritten().size(), generatedDir);
        return report;
    }

    private Map<String, Object> buildDocument(
            InventorySnapshot.ProgramInventory program,
            Path resources,
            Path javaRoot,
            Path samplesDir,
            String schemaText) throws IOException {
        String programId = program.getProgramId();
        String major = programId.substring(0, 2).toUpperCase(Locale.ROOT);
        String business = programId.substring(2, 4).toUpperCase(Locale.ROOT);
        String function = programId.substring(4, 5).toUpperCase(Locale.ROOT);
        String identifier = programId.substring(5, 9);

        Set<String> serviceIds = new LinkedHashSet<>(program.getServiceIds());
        if (serviceIds.isEmpty()) {
            for (String sqlId : program.getSqlIds()) {
                if (sqlId.length() >= 11 && sqlId.startsWith(programId)) {
                    serviceIds.add(sqlId.substring(0, 11));
                }
            }
        }

        List<String> tables = new ArrayList<>();
        if (program.getMapperXml() != null) {
            Path mapper = resources.resolve(program.getMapperXml());
            if (Files.isRegularFile(mapper)) {
                tables = extractTables(Files.readString(mapper, StandardCharsets.UTF_8));
            }
        }

        Object pk = null;
        if (!tables.isEmpty()) {
            pk = extractPk(schemaText, tables.get(0));
        }

        List<Map<String, Object>> services = new ArrayList<>();
        for (String serviceId : serviceIds) {
            Map<String, Object> svc = new LinkedHashMap<>();
            svc.put("serviceId", serviceId);
            svc.put("op", serviceId.substring(9, 10));
            svc.put("method", serviceId);
            String dtoIn = program.getPackageRoot() + ".dto." + serviceId + "DTOin";
            String dtoOut = program.getPackageRoot() + ".dto." + serviceId + "DTOout";
            if (javaClassExists(javaRoot, dtoIn)) {
                svc.put("dtoIn", dtoIn);
            }
            if (javaClassExists(javaRoot, dtoOut)) {
                svc.put("dtoOut", dtoOut);
            }
            List<String> sqlIds = program.getSqlIds().stream()
                    .filter(id -> id.startsWith(serviceId))
                    .toList();
            svc.put("sqlIds", sqlIds);
            services.add(svc);
        }

        List<String> samples = new ArrayList<>();
        if (Files.isDirectory(samplesDir)) {
            try (Stream<Path> list = Files.list(samplesDir)) {
                list.map(p -> p.getFileName().toString())
                        .filter(name -> name.startsWith(programId + "-") && name.endsWith(".json"))
                        .sorted()
                        .map(name -> "sample-requests/" + name)
                        .forEach(samples::add);
            }
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("@context", Map.of("@vocab", "https://nsight.local/ontology#"));
        doc.put("@id", "nsight:mapping-" + programId);
        doc.put("@type", "ProgramMapping");
        doc.put("version", "0.1.0-draft");
        doc.put("programId", programId);
        doc.put("title", programId);
        doc.put("majorGroup", major);
        doc.put("businessCode", business);
        doc.put("functionCode", function);
        doc.put("functionName", guessFunctionName(business, function));
        doc.put("identifier", identifier);
        doc.put("packageRoot", program.getPackageRoot());
        doc.put("modules", Map.of(
                "fw", "pdmg-fw",
                "service", "pdmg-service",
                "ui", "pdmg-ui"));
        doc.put("architecture", Map.of(
                "runtimeRef", "nsight:technical-tx-runtime",
                "txBoundary", "inside-worker-tx",
                "notes", List.of("auto-generated draft — review title/pk/exceptionCodes")));
        Map<String, Object> development = new LinkedHashMap<>();
        development.put("handler", program.getHandler());
        development.put("facade", program.getFacade());
        development.put("controller", program.getController());
        development.put("service", program.getService());
        development.put("dao", program.getDao());
        doc.put("development", development);
        doc.put("services", services);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mapperXml", program.getMapperXml());
        data.put("namespace", program.getDao());
        if (!tables.isEmpty()) {
            data.put("table", tables.size() == 1 ? tables.get(0) : tables);
        }
        if (pk != null) {
            data.put("pk", pk);
        }
        data.put("deleteMode", serviceIds.stream().anyMatch(id -> id.contains("D")) ? "physical" : "n/a");
        doc.put("data", data);

        Map<String, Object> operations = new LinkedHashMap<>();
        operations.put("uiRoute", "/" + programId + "/index.html");
        operations.put("shellView", programId);
        operations.put("samples", samples);
        operations.put("exceptionCodes", List.of("MP0404"));
        operations.put("envelope", Map.of(
                "success", "{ hdr_nhnis, dto }",
                "error", "{ hdr_nhnis, result }"));
        operations.put("configKeys", List.of(
                "nhnis.fw.tcf.enabled",
                "nhnis.fw.timeout.enabled"));
        doc.put("operations", operations);
        doc.put("rulesRef", List.of(
                "R-HANDLER-NO-DAO",
                "R-TX-OWNER-EXECUTOR",
                "R-NAMING-SERVICEID-METHOD",
                "R-PROGRAM-SINGLE-HANDLER"));
        doc.put("shapesRef", List.of("nsight:shape-service-id"));
        doc.put("generated", true);
        return doc;
    }

    private static List<String> extractTables(String xml) {
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        Matcher m = TABLE_REF.matcher(xml);
        while (m.find()) {
            tables.add(m.group(1).toUpperCase(Locale.ROOT));
        }
        return new ArrayList<>(tables);
    }

    private static Object extractPk(String schemaText, String table) {
        if (schemaText.isBlank() || table == null) {
            return null;
        }
        int idx = schemaText.indexOf("CREATE TABLE " + table);
        if (idx < 0) {
            return null;
        }
        int end = schemaText.indexOf(";", idx);
        String block = end > idx ? schemaText.substring(idx, end) : schemaText.substring(idx);
        Matcher m = PK_HINT.matcher(block);
        if (!m.find()) {
            return null;
        }
        String[] cols = m.group(1).split(",");
        List<String> pk = new ArrayList<>();
        for (String col : cols) {
            pk.add(col.trim());
        }
        return pk.size() == 1 ? pk.get(0) : pk;
    }

    private static boolean javaClassExists(Path javaRoot, String fqcn) {
        Path path = javaRoot.resolve(fqcn.replace('.', '/') + ".java");
        return Files.isRegularFile(path);
    }

    private static String guessFunctionName(String business, String function) {
        if ("CO".equals(business)) {
            return switch (function) {
                case "A" -> "공통관리";
                case "B" -> "사용자관리";
                case "C" -> "메뉴관리";
                case "D" -> "로그관리";
                default -> function;
            };
        }
        return function;
    }

    private void writeYaml(Path out, Map<String, Object> doc) throws IOException {
        Files.createDirectories(out.getParent());
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        String yaml = new Yaml(options).dump(doc);
        Files.writeString(out, yaml, StandardCharsets.UTF_8);
    }

    private Path resolve(String configured) {
        Path p = Path.of(configured);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return Path.of(System.getProperty("user.dir")).resolve(p).normalize();
    }

    private static String relativize(Path absolute) {
        Path root = Path.of(System.getProperty("user.dir")).normalize();
        try {
            return root.relativize(absolute).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return absolute.toString().replace('\\', '/');
        }
    }

    @Getter
    public static class SeedReport {
        private int programCount;
        private final List<String> created = new ArrayList<>();
        private final List<String> skippedExisting = new ArrayList<>();
        private final List<String> overwritten = new ArrayList<>();
        private final List<String> generated = new ArrayList<>();

        void setProgramCount(int programCount) {
            this.programCount = programCount;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("programCount", programCount);
            out.put("created", created);
            out.put("skippedExisting", skippedExisting);
            out.put("overwritten", overwritten);
            out.put("generated", generated);
            out.put("status", "OK");
            return out;
        }
    }
}
