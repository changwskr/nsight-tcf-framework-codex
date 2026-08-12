package nhnis.ontology.evidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.ontology.config.OntologyProperties;
import nhnis.ontology.domain.Provenance;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.store.OntologyStore;

/**
 * Minimal Scanner → Graph Provenance upgrade loop (P1-03).
 * When a SOURCE_CODE/DISCOVERED concept path exists on disk under scan roots, upgrade to VERIFIED.
 */
@Service
public class OntologyEvidenceMerger {

    private final OntologyStore store;
    private final OntologyProperties properties;

    public OntologyEvidenceMerger(OntologyStore store, OntologyProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    public Map<String, Object> upgradeFromFilesystem() {
        List<Map<String, Object>> upgraded = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        Path base = Path.of(System.getProperty("user.dir")).normalize();
        List<Path> roots = List.of(
                base.resolve(properties.getScan().getPdmgService()).normalize(),
                base.resolve(properties.getScan().getPdmgFw()).normalize());

        for (OntologyConcept c : store.findConceptsByType(ConceptType.COMPONENT)) {
            Provenance p = c.getProvenance();
            if (p == null || p.getSourcePath() == null) {
                skipped.add(Map.of("id", c.getId(), "reason", "no_provenance_path"));
                continue;
            }
            if (p.getVerificationStatus() == Provenance.VerificationStatus.VERIFIED
                    || p.getVerificationStatus() == Provenance.VerificationStatus.APPROVED) {
                continue;
            }
            Path found = resolveExisting(roots, p.getSourcePath());
            if (found == null) {
                skipped.add(Map.of("id", c.getId(), "reason", "file_not_found", "path", p.getSourcePath()));
                continue;
            }
            OntologyConcept verified = OntologyConcept.builder()
                    .id(c.getId())
                    .type(c.getType())
                    .name(c.getName())
                    .description(c.getDescription())
                    .attributes(c.getAttributes())
                    .version(c.getVersion())
                    .status(c.getStatus())
                    .provenance(Provenance.scannerVerified(found.toString(), "OntologyEvidenceMerger"))
                    .build();
            store.putConcept(verified);
            upgraded.add(Map.of(
                    "id", c.getId(),
                    "sourcePath", found.toString(),
                    "verificationStatus", "VERIFIED"));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "OK");
        out.put("upgradedCount", upgraded.size());
        out.put("skippedCount", skipped.size());
        out.put("upgraded", upgraded);
        out.put("note", "Closed loop: filesystem presence → Provenance VERIFIED (not full AST/Java parse)");
        return out;
    }

    private static Path resolveExisting(List<Path> roots, String sourcePath) {
        String normalized = sourcePath.replace('\\', '/');
        // strip leading module prefix like pdmg-service/
        String relative = normalized;
        int slash = normalized.indexOf('/');
        if (slash > 0 && (normalized.startsWith("pdmg-service/") || normalized.startsWith("pdmg-fw/"))) {
            relative = normalized.substring(slash + 1);
        }
        for (Path root : roots) {
            Path candidate = root.resolve(relative).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            Path direct = root.resolve(normalized).normalize();
            if (Files.isRegularFile(direct)) {
                return direct;
            }
        }
        Path asIs = Path.of(sourcePath);
        return Files.isRegularFile(asIs) ? asIs : null;
    }
}
