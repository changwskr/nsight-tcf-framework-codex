package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina9300DAO {
    List<Map<String, Object>> checklistOpen(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> haGaps(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> capacityGaps(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> waveGaps(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> validatingGroups(Map<String, Object> input) throws Exception;
}
