package nhnis.ontology.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "nhnis.ontology")
public class OntologyProperties {

    /** ontology/ 루트 상대 경로 */
    private String basePath = "ontology";

    /** 버전 스냅샷 경로 */
    private String versionsPath = "ontology/versions";

    /** Architecture knowledge markdown corpus (exearchidoc) */
    private String knowledgePath = "docs/knowledge/exearchidoc";

    /** pdmg-* 소스 스캔 경로 (프로젝트 상대) */
    private Scan scan = new Scan();

    /**
     * When false, mutation APIs (reload/import/seed) are blocked.
     * Safe default is false; enable only under local/dev profiles.
     */
    private boolean adminMutationsEnabled = false;

    @Getter
    @Setter
    public static class Scan {
        private String pdmgService = "../pdmg-service";
        private String pdmgUi = "../pdmg-ui";
        private String pdmgFw = "../pdmg-fw";
        private String importOutput = "test-data/ontology/inventory-pdmg.yml";
        private String reportOutput = "test-data/queries/last-validation-report.json";
    }

    public Map<String, String> moduleRoots() {
        Map<String, String> roots = new LinkedHashMap<>();
        roots.put("pdmg-service", scan.getPdmgService());
        roots.put("pdmg-ui", scan.getPdmgUi());
        roots.put("pdmg-fw", scan.getPdmgFw());
        return roots;
    }
}
