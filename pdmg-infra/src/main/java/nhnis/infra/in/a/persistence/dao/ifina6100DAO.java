package nhnis.infra.in.a.persistence.dao;

import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina6100DAO {
    Map<String, Object> ifina6100S0_S1(Map<String, Object> input) throws Exception;
    int ifina6100S0_exists(Map<String, Object> input) throws Exception;
    int ifina6100U0_insert(Map<String, Object> input) throws Exception;
    int ifina6100U0_update(Map<String, Object> input) throws Exception;
}
