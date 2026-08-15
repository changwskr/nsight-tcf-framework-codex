package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina4300DAO {
    List<Map<String, Object>> ifina4300S0_S0(Map<String, Object> input) throws Exception;
    int ifina4300S0_S0_count(Map<String, Object> input) throws Exception;
}
