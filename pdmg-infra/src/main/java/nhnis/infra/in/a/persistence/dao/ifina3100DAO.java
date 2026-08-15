package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina3100DAO {
    List<Map<String, Object>> ifina3100S0_S0(Map<String, Object> input) throws Exception;
    int ifina3100S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina3100S0_S0_exists(Map<String, Object> input) throws Exception;
    Map<String, Object> ifina3100S1_S1(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina3100S1_endpoints(Map<String, Object> input) throws Exception;
    int ifina3100S1_mw_count(Map<String, Object> input) throws Exception;
    int ifina3100S1_db_count(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina3100S1_attrs(Map<String, Object> input) throws Exception;
    int ifina3100C0_C0(Map<String, Object> input) throws Exception;
    int ifina3100U0_U0(Map<String, Object> input) throws Exception;
    int ifina3100U0_attr_exists(Map<String, Object> input) throws Exception;
    int ifina3100U0_attr_insert(Map<String, Object> input) throws Exception;
    int ifina3100U0_attr_update(Map<String, Object> input) throws Exception;
    int ifina3100D0_retire(Map<String, Object> input) throws Exception;
    int ifina3100D0_D0(Map<String, Object> input) throws Exception;
    int ifina3100D0_attr_delete(Map<String, Object> input) throws Exception;
}
