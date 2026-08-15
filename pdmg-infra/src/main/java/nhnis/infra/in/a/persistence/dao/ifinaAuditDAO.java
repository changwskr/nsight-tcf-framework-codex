package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifinaAuditDAO {
    int ifinaChangeLog_insert(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina1600S0_S0(Map<String, Object> input) throws Exception;
    int ifina1600S0_S0_count(Map<String, Object> input) throws Exception;
    int ifinaEvidence_insert(Map<String, Object> input) throws Exception;
    int ifinaEvidence_exists(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina1600S0_evidence(Map<String, Object> input) throws Exception;
    int ifina1600S0_evidence_count(Map<String, Object> input) throws Exception;
}
