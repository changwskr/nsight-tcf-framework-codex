package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina9200DAO {
    List<Map<String, Object>> ifina9200S0_defs(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina9200S0_results(Map<String, Object> input) throws Exception;
    Map<String, Object> ifina9200S0_one(Map<String, Object> input) throws Exception;
    int ifina9200U0_insert(Map<String, Object> input) throws Exception;
    int ifina9200U0_update(Map<String, Object> input) throws Exception;
}
