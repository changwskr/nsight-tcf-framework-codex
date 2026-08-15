package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina6200DAO {
    List<Map<String, Object>> ifina6200S0_S0(Map<String, Object> input) throws Exception;
    Map<String, Object> ifina6200S0_S1(Map<String, Object> input) throws Exception;
    int ifina6200S0_exists(Map<String, Object> input) throws Exception;
    int ifina6200U0_insert(Map<String, Object> input) throws Exception;
    int ifina6200U0_update(Map<String, Object> input) throws Exception;
}
