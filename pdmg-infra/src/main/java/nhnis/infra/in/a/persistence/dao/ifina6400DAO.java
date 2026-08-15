package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina6400DAO {
    List<Map<String, Object>> ifina6400S0_S0(Map<String, Object> input) throws Exception;
}
