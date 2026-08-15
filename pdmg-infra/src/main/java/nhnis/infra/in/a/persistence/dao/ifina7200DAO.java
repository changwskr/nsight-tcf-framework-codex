package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina7200DAO {
    Map<String, Object> ifina7200S0_license(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina7200S0_alloc(Map<String, Object> input) throws Exception;
    int ifina7200U0_deleteAll(Map<String, Object> input) throws Exception;
    int ifina7200U0_upsert(Map<String, Object> input) throws Exception;
}
