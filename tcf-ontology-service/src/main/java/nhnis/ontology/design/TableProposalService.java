package nhnis.ontology.design;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.store.OntologyStore;

/**
 * NEW_TABLE_PROPOSAL use cases for Design Wizard STEP 4.
 * Proposals stay PROPOSED — never VERIFIED Ontology facts.
 */
@Service
public class TableProposalService {

    private static final Pattern PHYSICAL_TABLE = Pattern.compile("^[A-Z][A-Z0-9_]{1,62}$");
    private static final Pattern PHYSICAL_COLUMN = Pattern.compile("^[A-Z][A-Z0-9_]{0,62}$");
    private static final List<String> FORBIDDEN_PREFIX = List.of("TMP_", "TEMP_", "TEST_");

    private final OntologyStore store;
    private final ConcurrentHashMap<String, Map<String, Object>> proposals = new ConcurrentHashMap<>();

    public TableProposalService(OntologyStore store) {
        this.store = store;
    }

    public Map<String, Object> validate(Map<String, Object> raw) {
        Map<String, Object> proposal = normalize(raw);
        List<Map<String, Object>> findings = new ArrayList<>();
        long fail = 0;
        long warn = 0;
        long unresolved = 0;

        String physical = str(proposal.get("physicalName"));
        String logical = str(proposal.get("logicalName"));
        String schema = str(proposal.get("schema"));
        String system = str(proposal.get("system"));
        String business = str(proposal.get("business"));
        String function = str(proposal.get("function"));

        if (blank(logical)) {
            findings.add(f("DATA-TBL-005", "FAIL", "logicalName", "논리 테이블명 필수"));
            fail++;
        } else {
            findings.add(f("DATA-TBL-005", "PASS", "logicalName", "논리명 OK"));
        }

        if (blank(physical)) {
            findings.add(f("DATA-TBL-001", "FAIL", "physicalName", "물리 테이블명 필수"));
            fail++;
        } else if (!PHYSICAL_TABLE.matcher(physical).matches()) {
            findings.add(f("DATA-TBL-002", "FAIL", physical, "물리명은 대문자/숫자/_ , 최대 63자"));
            fail++;
        } else if (FORBIDDEN_PREFIX.stream().anyMatch(physical::startsWith)) {
            findings.add(f("TABLE-NAME-006", "FAIL", physical, "금지 Prefix"));
            fail++;
        } else {
            findings.add(f("DATA-TBL-002", "PASS", physical, "물리명 형식 OK"));
        }

        if (!blank(physical) && ontologyTableExists(physical)) {
            findings.add(f("DATA-TBL-003", "FAIL", physical, "TABLE_ALREADY_EXISTS in Ontology"));
            fail++;
        } else if (!blank(physical)) {
            findings.add(f("DATA-TBL-003", "PASS", physical, "Ontology 중복 없음"));
        }

        if (!blank(physical) && proposalDuplicate(physical, str(proposal.get("proposalId")))) {
            findings.add(f("DATA-TBL-004", "FAIL", physical, "Proposal 중복"));
            fail++;
        }

        if (blank(schema) || blank(system) || blank(business) || blank(function)) {
            findings.add(f("DATA-TBL-006", "FAIL", "classification", "Schema/System/Business/Function 필수"));
            fail++;
        } else {
            findings.add(f("DATA-TBL-006", "PASS", "classification", "업무분류 OK"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) proposal.get("columns");
        if (columns == null || columns.isEmpty()) {
            findings.add(f("DATA-COL-001", "FAIL", "columns", "AT_LEAST_ONE_COLUMN_REQUIRED"));
            fail++;
        } else {
            findings.add(f("DATA-COL-001", "PASS", "columns", "Column " + columns.size() + "건"));
            fail += validateColumns(columns, findings);
            unresolved += countColumnUnresolved(columns);
        }

        @SuppressWarnings("unchecked")
        List<String> pk = (List<String>) proposal.get("primaryKey");
        if (pk == null || pk.isEmpty()) {
            findings.add(f("DATA-PK-001", "UNRESOLVED", "primaryKey", "PK 미지정 — Architect/DA 검토 필요"));
            unresolved++;
        } else if (columns != null) {
            for (String pkCol : pk) {
                Map<String, Object> col = findColumn(columns, pkCol);
                if (col == null) {
                    findings.add(f("DATA-PK-002", "FAIL", pkCol, "PK Column이 columns에 없음"));
                    fail++;
                } else if (Boolean.TRUE.equals(col.get("nullable"))) {
                    findings.add(f("DATA-PK-003", "FAIL", pkCol, "PK는 nullable=false 여야 함"));
                    fail++;
                }
            }
            if (pk.size() > 1) {
                findings.add(f("DATA-PK-004", "PASS", String.join(",", pk), "Composite PK 지원"));
            }
        }

        String personal = str(proposal.get("hasPersonalData"));
        if (blank(personal) || "UNKNOWN".equalsIgnoreCase(personal) || "UNRESOLVED".equalsIgnoreCase(personal)) {
            findings.add(f("DATA-SEC-001", "UNRESOLVED", "hasPersonalData", "개인정보 여부 UNRESOLVED"));
            unresolved++;
        }

        String status = fail > 0 ? "FAIL" : (unresolved > 0 || warn > 0 ? "PASS_WITH_UNRESOLVED" : "PASS");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scope", "NEW_TABLE_PROPOSAL");
        out.put("status", status);
        out.put("failCount", fail);
        out.put("warnCount", warn);
        out.put("unresolvedCount", unresolved);
        out.put("findings", findings);
        out.put("proposal", proposal);
        out.put("note", "Validation only — does not create VERIFIED Ontology Table");
        return out;
    }

    public Map<String, Object> create(Map<String, Object> raw) {
        Map<String, Object> validation = validate(raw);
        if ("FAIL".equals(validation.get("status"))) {
            Map<String, Object> rejected = new LinkedHashMap<>(validation);
            rejected.put("proposalStatus", "DRAFT");
            rejected.put("accepted", false);
            return rejected;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> proposal = new LinkedHashMap<>((Map<String, Object>) validation.get("proposal"));
        String id = UUID.randomUUID().toString();
        proposal.put("proposalId", id);
        proposal.put("mode", "NEW_TABLE_PROPOSAL");
        proposal.put("status", "PROPOSED");
        proposal.put("ontologyStatus", "PROPOSED");
        proposal.put("verificationStatus", "PROPOSED");
        proposal.put("accepted", true);
        proposal.put("createdAt", java.time.Instant.now().toString());
        proposals.put(id, proposal);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accepted", true);
        out.put("proposalId", id);
        out.put("proposalStatus", "PROPOSED");
        out.put("validation", validation);
        out.put("proposal", proposal);
        out.put("note", "Stored as PROPOSED proposal only — not Ontology VERIFIED");
        return out;
    }

    public Map<String, Object> get(String proposalId) {
        Map<String, Object> p = proposals.get(proposalId);
        if (p == null) {
            throw new IllegalArgumentException("proposal not found: " + proposalId);
        }
        return p;
    }

    public Map<String, Object> update(String proposalId, Map<String, Object> raw) {
        if (!proposals.containsKey(proposalId)) {
            throw new IllegalArgumentException("proposal not found: " + proposalId);
        }
        Map<String, Object> body = raw == null ? Map.of() : new LinkedHashMap<>(raw);
        body.put("proposalId", proposalId);
        Map<String, Object> validation = validate(body);
        if ("FAIL".equals(validation.get("status"))) {
            Map<String, Object> rejected = new LinkedHashMap<>(validation);
            rejected.put("accepted", false);
            rejected.put("proposalId", proposalId);
            return rejected;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> proposal = new LinkedHashMap<>((Map<String, Object>) validation.get("proposal"));
        proposal.put("proposalId", proposalId);
        proposal.put("mode", "NEW_TABLE_PROPOSAL");
        proposal.put("status", "PROPOSED");
        proposal.put("ontologyStatus", "PROPOSED");
        proposal.put("verificationStatus", "PROPOSED");
        proposal.put("updatedAt", java.time.Instant.now().toString());
        proposals.put(proposalId, proposal);
        return Map.of(
                "accepted", true,
                "proposalId", proposalId,
                "proposalStatus", "PROPOSED",
                "validation", validation,
                "proposal", proposal);
    }

    public List<Map<String, Object>> list() {
        return new ArrayList<>(proposals.values());
    }

    private long validateColumns(List<Map<String, Object>> columns, List<Map<String, Object>> findings) {
        long fail = 0;
        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (Map<String, Object> col : columns) {
            String logical = str(col.get("logicalName"));
            String physical = str(col.get("physicalName")).toUpperCase(Locale.ROOT);
            String dataType = str(col.get("dataType")).toUpperCase(Locale.ROOT);
            if (blank(logical)) {
                findings.add(f("COL-001", "FAIL", physical, "Column 논리명 필수"));
                fail++;
            }
            if (blank(physical) || !PHYSICAL_COLUMN.matcher(physical).matches()) {
                findings.add(f("COL-002", "FAIL", physical, "Column 물리명 필수/형식"));
                fail++;
            } else if (!names.add(physical)) {
                findings.add(f("COL-003", "FAIL", physical, "Column 물리명 중복"));
                fail++;
            }
            if (blank(dataType)) {
                findings.add(f("COL-004", "FAIL", physical, "Data Type 필수"));
                fail++;
            } else if ((dataType.contains("CHAR") || dataType.equals("VARCHAR2") || dataType.equals("VARCHAR"))
                    && blank(str(col.get("length")))) {
                findings.add(f("COL-005", "FAIL", physical, "VARCHAR/CHAR Length 필수"));
                fail++;
            }
            if (Boolean.TRUE.equals(col.get("primaryKey")) && Boolean.TRUE.equals(col.get("nullable"))) {
                findings.add(f("COL-007", "FAIL", physical, "PK는 nullable=false"));
                fail++;
            }
            String pd = str(col.get("personalData"));
            if (blank(pd)) {
                col.put("personalData", "UNRESOLVED");
            }
            String enc = str(col.get("encryption"));
            if (blank(enc)) {
                col.put("encryption", "UNRESOLVED");
            }
            String mask = str(col.get("masking"));
            if (blank(mask)) {
                col.put("masking", "UNRESOLVED");
            }
            col.put("physicalName", physical);
            col.put("dataType", dataType);
        }
        return fail;
    }

    private long countColumnUnresolved(List<Map<String, Object>> columns) {
        long n = 0;
        for (Map<String, Object> col : columns) {
            for (String k : List.of("personalData", "encryption", "masking")) {
                if ("UNRESOLVED".equalsIgnoreCase(str(col.get(k))) || "UNKNOWN".equalsIgnoreCase(str(col.get(k)))) {
                    n++;
                }
            }
        }
        return n;
    }

    private Map<String, Object> normalize(Map<String, Object> raw) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (raw == null) {
            return p;
        }
        raw.forEach((k, v) -> {
            if (v != null) {
                p.put(k, v);
            }
        });
        String physical = str(p.get("physicalName")).toUpperCase(Locale.ROOT).trim();
        p.put("physicalName", physical);
        p.put("schema", defaultVal(str(p.get("schema")), "RDW").toUpperCase(Locale.ROOT));
        p.put("system", defaultVal(str(p.get("system")), "MG").toUpperCase(Locale.ROOT));
        p.put("business", defaultVal(str(p.get("business")), "CO").toUpperCase(Locale.ROOT));
        p.put("function", defaultVal(str(p.get("function")), "A").toUpperCase(Locale.ROOT));
        p.put("tableType", defaultVal(str(p.get("tableType")), "MASTER").toUpperCase(Locale.ROOT));
        p.put("accessType", defaultVal(str(p.get("accessType")), "READ").toUpperCase(Locale.ROOT));
        p.put("mode", "NEW_TABLE_PROPOSAL");
        if (!p.containsKey("status")) {
            p.put("status", "DRAFT");
        }
        if (!(p.get("columns") instanceof List<?>)) {
            p.put("columns", new ArrayList<>());
        } else {
            List<Map<String, Object>> cols = new ArrayList<>();
            for (Object item : (List<?>) p.get("columns")) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    m.forEach((k, v) -> c.put(String.valueOf(k), v));
                    cols.add(c);
                }
            }
            p.put("columns", cols);
        }
        // derive PK list from columns.primaryKey flags if not provided as list
        if (!(p.get("primaryKey") instanceof List<?>)) {
            List<String> pk = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cols = (List<Map<String, Object>>) p.get("columns");
            for (Map<String, Object> c : cols) {
                if (Boolean.TRUE.equals(c.get("primaryKey")) || "true".equalsIgnoreCase(str(c.get("primaryKey")))) {
                    c.put("primaryKey", true);
                    c.put("nullable", false);
                    pk.add(str(c.get("physicalName")).toUpperCase(Locale.ROOT));
                } else {
                    c.put("primaryKey", false);
                }
            }
            p.put("primaryKey", pk);
        } else {
            List<String> pk = new ArrayList<>();
            for (Object item : (List<?>) p.get("primaryKey")) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    pk.add(String.valueOf(item).toUpperCase(Locale.ROOT).trim());
                }
            }
            p.put("primaryKey", pk);
        }
        if (!(p.get("indexes") instanceof List<?>)) {
            p.put("indexes", List.of());
        }
        if (!(p.get("relations") instanceof List<?>)) {
            p.put("relations", List.of());
        }
        if (blank(str(p.get("hasPersonalData")))) {
            p.put("hasPersonalData", "UNRESOLVED");
        }
        return p;
    }

    private boolean ontologyTableExists(String physicalName) {
        return store.findConceptOfType(physicalName, ConceptType.TABLE).isPresent()
                || store.findConceptsByType(ConceptType.TABLE).stream()
                        .anyMatch(c -> physicalName.equalsIgnoreCase(c.getName()));
    }

    private boolean proposalDuplicate(String physicalName, String excludeId) {
        for (Map<String, Object> p : proposals.values()) {
            if (excludeId != null && excludeId.equals(p.get("proposalId"))) {
                continue;
            }
            if (physicalName.equalsIgnoreCase(str(p.get("physicalName")))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> findColumn(List<Map<String, Object>> columns, String name) {
        for (Map<String, Object> c : columns) {
            if (name.equalsIgnoreCase(str(c.get("physicalName")))) {
                return c;
            }
        }
        return null;
    }

    private static Map<String, Object> f(String ruleId, String verdict, String target, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ruleId", ruleId);
        m.put("verdict", verdict);
        m.put("target", target);
        m.put("message", message);
        return m;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String defaultVal(String v, String fallback) {
        return blank(v) ? fallback : v;
    }
}
