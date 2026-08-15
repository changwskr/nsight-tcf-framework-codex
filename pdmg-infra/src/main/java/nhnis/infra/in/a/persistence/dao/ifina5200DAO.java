package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina5200DAO {
    List<Map<String, Object>> ifina5200S0_S0(Map<String, Object> input) throws Exception;
    int ifina5200S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina5200S0_exists(Map<String, Object> input) throws Exception;
    int ifina5200S0_appExists(Map<String, Object> input) throws Exception;
    Map<String, Object> ifina5200S0_S1(Map<String, Object> input) throws Exception;
    int ifina5200C0_C0(Map<String, Object> input) throws Exception;
    int ifina5200U0_U0(Map<String, Object> input) throws Exception;
    int ifina5200D0_D0(Map<String, Object> input) throws Exception;
}
