package nhnis.infra.in.a.persistence.dao;

import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina6300DAO {
    Map<String, Object> ifina6300S0_S1(Map<String, Object> input) throws Exception;
    int ifina6300S0_exists(Map<String, Object> input) throws Exception;
    int ifina6300U0_insert(Map<String, Object> input) throws Exception;
    int ifina6300U0_update(Map<String, Object> input) throws Exception;
}
