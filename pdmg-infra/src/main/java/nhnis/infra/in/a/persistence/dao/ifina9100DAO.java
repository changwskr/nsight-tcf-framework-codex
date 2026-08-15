package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina9100DAO {
    List<Map<String, Object>> ifina9100S0_S0(Map<String, Object> input) throws Exception;
    int ifina9100U0_U0_exists(Map<String, Object> input) throws Exception;
    int ifina9100U0_U0_insert(Map<String, Object> input) throws Exception;
    int ifina9100U0_U0_update(Map<String, Object> input) throws Exception;
}
