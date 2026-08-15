package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina8300DAO {
    List<Map<String, Object>> ifina8300S0_S0(Map<String, Object> input) throws Exception;
    int ifina8300S0_S0_count(Map<String, Object> input) throws Exception;
}
