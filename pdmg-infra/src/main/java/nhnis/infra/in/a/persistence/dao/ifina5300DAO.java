package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina5300DAO {
    List<Map<String, Object>> ifina5300S0_S0(Map<String, Object> input) throws Exception;
    int ifina5300S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina5300S0_S0_exists(Map<String, Object> input) throws Exception;
    int ifina5300C0_C0(Map<String, Object> input) throws Exception;
    int ifina5300D0_D0(Map<String, Object> input) throws Exception;
}
