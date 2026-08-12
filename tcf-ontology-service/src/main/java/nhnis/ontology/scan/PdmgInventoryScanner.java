package nhnis.ontology.scan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;
import nhnis.ontology.config.OntologyProperties;

@Slf4j
@Service
public class PdmgInventoryScanner {

    private static final Pattern PROGRAM_CLASS = Pattern.compile(
            "^(?<program>[a-z]{2}[a-z]{2}[a-z][0-9]{4})(?<kind>Handler|Facade|Controller|Service|DAO)\\.java$");
    private static final Pattern SERVICE_IDS = Pattern.compile(
            "List\\.of\\(([^)]*)\\)");
    private static final Pattern QUOTED = Pattern.compile("\"([a-z0-9]+)\"");
    private static final Pattern STRING_CONST = Pattern.compile(
            "(?:private\\s+)?static\\s+final\\s+String\\s+\\w+\\s*=\\s*\"([a-z0-9]+)\"");
    private static final Pattern SQL_ID = Pattern.compile(
            "<(select|insert|update|delete)\\s+[^>]*id=\"([^\"]+)\"");
    private static final Pattern MAPPER_NS = Pattern.compile("namespace=\"([^\"]+)\"");
    private static final Pattern PACKAGE_DECL = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern SERVICE_ID_VALUE = Pattern.compile(
            "^[a-z]{2}[a-z]{2}[a-z][0-9]{4}[SCUDAR][0-9A-Z]$");

    private final OntologyProperties properties;

    public PdmgInventoryScanner(OntologyProperties properties) {
        this.properties = properties;
    }

    public InventorySnapshot scan() throws IOException {
        InventorySnapshot snap = new InventorySnapshot();
        snap.setGeneratedAt(OffsetDateTime.now().toString());
        snap.setRoots(properties.moduleRoots());

        Path serviceRoot = resolve(properties.getScan().getPdmgService());
        Path uiRoot = resolve(properties.getScan().getPdmgUi());
        Path fwRoot = resolve(properties.getScan().getPdmgFw());

        Map<String, InventorySnapshot.ProgramInventory> programs = new LinkedHashMap<>();

        if (Files.isDirectory(serviceRoot)) {
            Path javaRoot = serviceRoot.resolve("src/main/java");
            if (Files.isDirectory(javaRoot)) {
                try (Stream<Path> walk = Files.walk(javaRoot)) {
                    walk.filter(p -> p.toString().endsWith(".java"))
                            .forEach(p -> collectJava(p, programs, snap));
                }
            }
            Path resources = serviceRoot.resolve("src/main/resources");
            if (Files.isDirectory(resources)) {
                try (Stream<Path> walk = Files.walk(resources)) {
                    walk.filter(p -> p.getFileName().toString().endsWith("-ORA.xml"))
                            .forEach(p -> collectMapper(p, resources, programs, snap));
                }
            }
        } else {
            snap.getNotes().add("pdmg-service path not found: " + serviceRoot);
        }

        if (Files.isDirectory(uiRoot)) {
            Path staticRoot = uiRoot.resolve("src/main/resources/static");
            if (Files.isDirectory(staticRoot)) {
                try (Stream<Path> walk = Files.walk(staticRoot)) {
                    walk.filter(p -> p.getFileName().toString().equals("index.html"))
                            .forEach(p -> {
                                Path rel = staticRoot.relativize(p.getParent());
                                String route = "/" + rel.toString().replace('\\', '/') + "/index.html";
                                if (route.equals("//index.html")) {
                                    route = "/index.html";
                                }
                                snap.getUiRoutes().add(route);
                            });
                }
            }
            Path samples = uiRoot.resolve("src/main/resources/sample-requests");
            if (Files.isDirectory(samples)) {
                try (Stream<Path> walk = Files.list(samples)) {
                    walk.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .map(p -> "sample-requests/" + p.getFileName())
                            .sorted()
                            .forEach(snap.getSampleRequests()::add);
                }
            }
        } else {
            snap.getNotes().add("pdmg-ui path not found: " + uiRoot);
        }

        if (Files.isDirectory(fwRoot)) {
            List<String> highlights = List.of(
                    "nhnis/fw/commons/filter/DefaultFilter.java",
                    "nhnis/fw/commons/interceptor/ServicePreventionInterceptor.java",
                    "nhnis/fw/tcf/web/OnlineTransactionController.java",
                    "nhnis/fw/tcf/core/facade/TcfFacade.java",
                    "nhnis/fw/tcf/timeout/DefaultOnlineTimeoutExecutor.java");
            for (String rel : highlights) {
                Path p = fwRoot.resolve("src/main/java").resolve(rel);
                if (Files.isRegularFile(p)) {
                    snap.getFwHighlights().add(rel.replace('/', '.').replace(".java", ""));
                }
            }
        } else {
            snap.getNotes().add("pdmg-fw path not found: " + fwRoot);
        }

        programs.values().stream()
                .sorted(Comparator.comparing(InventorySnapshot.ProgramInventory::getProgramId))
                .forEach(snap.getPrograms()::add);
        snap.getUiRoutes().sort(String::compareTo);
        return snap;
    }

    public Path writeYaml(InventorySnapshot snapshot) throws IOException {
        Path out = resolve(properties.getScan().getImportOutput());
        Files.createDirectories(out.getParent());

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("@id", "nsight:inventory-pdmg");
        doc.put("@type", "SourceInventory");
        doc.put("generatedAt", snapshot.getGeneratedAt());
        doc.put("roots", snapshot.getRoots());
        doc.put("notes", snapshot.getNotes());
        doc.put("fwHighlights", snapshot.getFwHighlights());
        doc.put("uiRoutes", snapshot.getUiRoutes());
        doc.put("sampleRequests", snapshot.getSampleRequests());

        List<Map<String, Object>> programs = new ArrayList<>();
        for (InventorySnapshot.ProgramInventory p : snapshot.getPrograms()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("programId", p.getProgramId());
            row.put("packageRoot", p.getPackageRoot());
            row.put("handler", p.getHandler());
            row.put("facade", p.getFacade());
            row.put("controller", p.getController());
            row.put("service", p.getService());
            row.put("dao", p.getDao());
            row.put("mapperXml", p.getMapperXml());
            row.put("serviceIds", new ArrayList<>(p.getServiceIds()));
            row.put("sqlIds", new ArrayList<>(p.getSqlIds()));
            programs.add(row);
        }
        doc.put("programs", programs);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        String yaml = new Yaml(options).dump(doc);
        Files.writeString(out, yaml, StandardCharsets.UTF_8);
        log.info("Wrote inventory YAML: {}", out.toAbsolutePath());
        return out;
    }

    private void collectJava(Path file, Map<String, InventorySnapshot.ProgramInventory> programs,
            InventorySnapshot snap) {
        String name = file.getFileName().toString();
        Matcher m = PROGRAM_CLASS.matcher(name);
        if (!m.matches()) {
            return;
        }
        String programId = m.group("program");
        String kind = m.group("kind");
        InventorySnapshot.ProgramInventory inv = programs.computeIfAbsent(programId, id -> {
            InventorySnapshot.ProgramInventory p = new InventorySnapshot.ProgramInventory();
            p.setProgramId(id);
            return p;
        });

        try {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            Matcher pkg = PACKAGE_DECL.matcher(source);
            if (pkg.find()) {
                String pkgName = pkg.group(1);
                inv.setPackageRoot(trimToBusinessRoot(pkgName));
                String fqcn = pkgName + "." + name.replace(".java", "");
                switch (kind) {
                    case "Handler" -> {
                        inv.setHandler(fqcn);
                        extractServiceIds(source).forEach(inv.getServiceIds()::add);
                    }
                    case "Facade" -> inv.setFacade(fqcn);
                    case "Controller" -> inv.setController(fqcn);
                    case "Service" -> inv.setService(fqcn);
                    case "DAO" -> inv.setDao(fqcn);
                    default -> {
                    }
                }
            }
        } catch (IOException e) {
            snap.getNotes().add("failed to read " + file + ": " + e.getMessage());
        }
    }

    private void collectMapper(Path file, Path resourcesRoot,
            Map<String, InventorySnapshot.ProgramInventory> programs, InventorySnapshot snap) {
        String fileName = file.getFileName().toString();
        String programId = fileName.replace("-ORA.xml", "");
        if (!programId.matches("[a-z]{2}[a-z]{2}[a-z][0-9]{4}")) {
            return;
        }
        InventorySnapshot.ProgramInventory inv = programs.computeIfAbsent(programId, id -> {
            InventorySnapshot.ProgramInventory p = new InventorySnapshot.ProgramInventory();
            p.setProgramId(id);
            return p;
        });
        Path rel = resourcesRoot.relativize(file);
        inv.setMapperXml(rel.toString().replace('\\', '/'));
        try {
            String xml = Files.readString(file, StandardCharsets.UTF_8);
            Matcher ns = MAPPER_NS.matcher(xml);
            if (ns.find() && inv.getDao() == null) {
                inv.setDao(ns.group(1));
            }
            Matcher sql = SQL_ID.matcher(xml);
            while (sql.find()) {
                inv.getSqlIds().add(sql.group(2));
            }
        } catch (IOException e) {
            snap.getNotes().add("failed to read mapper " + file + ": " + e.getMessage());
        }
    }

    private static List<String> extractServiceIds(String source) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Matcher constants = STRING_CONST.matcher(source);
        while (constants.find()) {
            String id = constants.group(1);
            if (SERVICE_ID_VALUE.matcher(id).matches()) {
                ids.add(id);
            }
        }
        Matcher list = SERVICE_IDS.matcher(source);
        while (list.find()) {
            Matcher q = QUOTED.matcher(list.group(1));
            while (q.find()) {
                String id = q.group(1);
                if (SERVICE_ID_VALUE.matcher(id).matches()) {
                    ids.add(id);
                }
            }
        }
        if (ids.isEmpty()) {
            Matcher any = Pattern.compile("\"([a-z]{2}[a-z]{2}[a-z][0-9]{4}[SCUDAR][0-9A-Z])\"")
                    .matcher(source);
            while (any.find()) {
                ids.add(any.group(1));
            }
        }
        return new ArrayList<>(ids);
    }

    private static String trimToBusinessRoot(String pkg) {
        // nhnis.mg.co.a.application.service -> nhnis.mg.co.a
        String[] parts = pkg.split("\\.");
        if (parts.length >= 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        }
        return pkg;
    }

    private Path resolve(String configured) {
        Path p = Path.of(configured);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return Path.of(System.getProperty("user.dir")).resolve(p).normalize();
    }

    public Optional<Path> lastImportPath() {
        Path out = resolve(properties.getScan().getImportOutput());
        return Files.isRegularFile(out) ? Optional.of(out) : Optional.empty();
    }
}
