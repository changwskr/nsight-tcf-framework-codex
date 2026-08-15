package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina1100DAO {
    List<Map<String, Object>> ifina1100S0_sets(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina1100S0_S0(Map<String, Object> input) throws Exception;
    int ifina1100S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina1100S0_S0_exists(Map<String, Object> input) throws Exception;
    int ifina1100S0_set_exists(Map<String, Object> input) throws Exception;
    int ifina1100C0_C0(Map<String, Object> input) throws Exception;
    int ifina1100U0_U0(Map<String, Object> input) throws Exception;
}
