package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina1400DAO {
    List<Map<String, Object>> ifina1400S0_S0(Map<String, Object> input) throws Exception;
    int ifina1400S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina1400S0_exists(Map<String, Object> input) throws Exception;
    Map<String, Object> ifina1400S0_S1(Map<String, Object> input) throws Exception;
    int ifina1400C0_C0(Map<String, Object> input) throws Exception;
    int ifina1400U0_U0(Map<String, Object> input) throws Exception;
}
