package nhnis.ontology.validate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import nhnis.ontology.ontology.OntologyRegistry;
import nhnis.ontology.scan.InventorySnapshot;
import nhnis.ontology.scan.PdmgInventoryScanner;

@Service
public class OntologyValidator {

    private final OntologyRegistry registry;
    private final PdmgInventoryScanner scanner;

    public OntologyValidator(OntologyRegistry registry, PdmgInventoryScanner scanner) {
        this.registry = registry;
        this.scanner = scanner;
    }

    public Map<String, Object> validate(InventorySnapshot inventory) {
        Pattern serviceIdPattern = patternFromShape("pattern",
                "^[a-z]{2}[a-z]{2}[a-z][0-9]{4}[SCUDAR][0-9A-Z]$");
        Pattern mapperPattern = patternFromNested("mapperAxis", "pattern",
                "^rdw\\.[a-z]{2}\\.[a-z]{2}\\.[a-z]/");

        List<Map<String, Object>> findings = new ArrayList<>();
        int errors = 0;
        int warnings = 0;

        for (InventorySnapshot.ProgramInventory program : inventory.getPrograms()) {
            String programId = program.getProgramId();

            if (program.getPackageRoot() != null
                    && !program.getPackageRoot().matches("^nhnis\\.[a-z]{2}\\.[a-z]{2}\\.[a-z]$")) {
                findings.add(finding("error", "SHAPE-PACKAGE", programId, null,
                        "packageRoot does not match axis: " + program.getPackageRoot()));
                errors++;
            }

            if (program.getPackageRoot() != null && programId.length() >= 5) {
                String expected = "nhnis."
                        + programId.substring(0, 2) + "."
                        + programId.substring(2, 4) + "."
                        + programId.substring(4, 5);
                if (!expected.equals(program.getPackageRoot())) {
                    findings.add(finding("error", "PKG-SERVICEID-AXIS", programId, null,
                            "expected package " + expected + " but was " + program.getPackageRoot()));
                    errors++;
                }
            }

            if (program.getMapperXml() != null) {
                String mapperDir = program.getMapperXml().contains("/")
                        ? program.getMapperXml().substring(0, program.getMapperXml().lastIndexOf('/') + 1)
                        : program.getMapperXml();
                if (!mapperPattern.matcher(mapperDir).find()) {
                    findings.add(finding("error", "SHAPE-MAPPER", programId, null,
                            "mapper path axis mismatch: " + program.getMapperXml()));
                    errors++;
                }
            }

            for (String serviceId : program.getServiceIds()) {
                if (!serviceIdPattern.matcher(serviceId).matches()) {
                    findings.add(finding("error", "SHAPE-SERVICEID", programId, serviceId,
                            "serviceId fails shape pattern"));
                    errors++;
                } else if (!serviceId.startsWith(programId)) {
                    findings.add(finding("error", "SERVICEID-PROGRAM", programId, serviceId,
                            "serviceId prefix != programId"));
                    errors++;
                }
            }

            boolean inOntology = registry.findProgram(programId).isPresent();
            if (!inOntology) {
                findings.add(finding("warning", "MAPPING-MISSING", programId, null,
                        "source program has no ontology/mappings/*.yml seed"));
                warnings++;
            } else {
                Map<String, Object> mapping = registry.findProgram(programId).orElseThrow();
                errors += compareField(findings, programId, "handler",
                        str(mapping, "development", "handler"), program.getHandler());
                errors += compareField(findings, programId, "facade",
                        str(mapping, "development", "facade"), program.getFacade());
                errors += compareField(findings, programId, "service",
                        str(mapping, "development", "service"), program.getService());
                errors += compareField(findings, programId, "dao",
                        str(mapping, "development", "dao"), program.getDao());
                String mappedTableMapper = str(mapping, "data", "mapperXml");
                if (mappedTableMapper != null && program.getMapperXml() != null
                        && !mappedTableMapper.equals(program.getMapperXml())) {
                    findings.add(finding("error", "MAPPER-DRIFT", programId, null,
                            "ontology mapperXml=" + mappedTableMapper
                                    + " source=" + program.getMapperXml()));
                    errors++;
                }
            }

            if (program.getHandler() != null && program.getServiceIds().isEmpty()) {
                findings.add(finding("warning", "HANDLER-NO-SERVICEIDS", programId, null,
                        "Handler found but serviceIds() not parsed"));
                warnings++;
            }
        }

        // ontology programs missing in source
        Object catalogPrograms = registry.catalog().get("programs");
        if (catalogPrograms instanceof List<?> seeded) {
            for (Object item : seeded) {
                String programId = String.valueOf(item);
                boolean found = inventory.getPrograms().stream()
                        .anyMatch(p -> programId.equals(p.getProgramId()));
                if (!found) {
                    findings.add(finding("warning", "SOURCE-MISSING", programId, null,
                            "ontology mapping exists but source program not scanned"));
                    warnings++;
                }
            }
        }

        // UI route hint for mapped programs
        for (InventorySnapshot.ProgramInventory program : inventory.getPrograms()) {
            String expected = "/" + program.getProgramId() + "/index.html";
            boolean hasUi = inventory.getUiRoutes().stream().anyMatch(r -> r.equals(expected));
            if (!hasUi && registry.findProgram(program.getProgramId()).isPresent()) {
                findings.add(finding("warning", "UI-ROUTE-MISSING", program.getProgramId(), null,
                        "expected UI route " + expected));
                warnings++;
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", inventory.getGeneratedAt());
        report.put("programCount", inventory.getPrograms().size());
        report.put("errorCount", errors);
        report.put("warningCount", warnings);
        report.put("status", errors == 0 ? (warnings == 0 ? "PASS" : "PASS_WITH_WARNINGS") : "FAIL");
        report.put("findings", findings);
        report.put("inventorySummary", Map.of(
                "programs", inventory.getPrograms().stream().map(InventorySnapshot.ProgramInventory::getProgramId).toList(),
                "uiRoutes", inventory.getUiRoutes(),
                "fwHighlights", inventory.getFwHighlights(),
                "notes", inventory.getNotes()));
        return report;
    }

    public Map<String, Object> scanAndValidate() throws Exception {
        InventorySnapshot inventory = scanner.scan();
        scanner.writeYaml(inventory);
        return validate(inventory);
    }

    private Pattern patternFromShape(String key, String fallback) {
        Map<String, Object> shape = registry.getBundle("shapes/service-id.yml");
        Object value = shape.get(key);
        return Pattern.compile(value != null ? String.valueOf(value) : fallback);
    }

    @SuppressWarnings("unchecked")
    private Pattern patternFromNested(String parent, String key, String fallback) {
        Map<String, Object> shape = registry.getBundle("shapes/service-id.yml");
        Object nested = shape.get(parent);
        if (nested instanceof Map<?, ?> map && map.get(key) != null) {
            return Pattern.compile(String.valueOf(map.get(key)));
        }
        return Pattern.compile(fallback);
    }

    private static Map<String, Object> finding(String severity, String code, String programId,
            String serviceId, String message) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("severity", severity);
        row.put("code", code);
        row.put("programId", programId);
        if (serviceId != null) {
            row.put("serviceId", serviceId);
        }
        row.put("message", message);
        return row;
    }

    private int compareField(List<Map<String, Object>> findings, String programId, String field,
            String expected, String actual) {
        if (expected == null || actual == null) {
            return 0;
        }
        if (!Objects.equals(expected, actual)) {
            findings.add(finding("error", "DEV-" + field.toUpperCase() + "-DRIFT", programId, null,
                    "ontology " + field + "=" + expected + " source=" + actual));
            return 1;
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static String str(Map<String, Object> root, String nested, String field) {
        Object n = root.get(nested);
        if (n instanceof Map<?, ?> map && map.get(field) != null) {
            return String.valueOf(map.get(field));
        }
        return null;
    }
}
