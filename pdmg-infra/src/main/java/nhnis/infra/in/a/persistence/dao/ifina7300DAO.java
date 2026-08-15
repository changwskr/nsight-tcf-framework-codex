package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina7300DAO {
    List<Map<String, Object>> ifina7300S0_S0(Map<String, Object> input) throws Exception;
    int ifina7300S0_exists(Map<String, Object> input) throws Exception;
    int ifina7300S0_countTarget(Map<String, Object> input) throws Exception;
    int ifina7300C0_C0(Map<String, Object> input) throws Exception;
}
