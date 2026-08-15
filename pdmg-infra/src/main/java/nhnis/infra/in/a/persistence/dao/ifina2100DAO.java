package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina2100DAO {
    List<Map<String, Object>> ifina2100S0_S0(Map<String, Object> input) throws Exception;
    int ifina2100S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina2100S0_S0_exists(Map<String, Object> input) throws Exception;
    int ifina2100C0_C0(Map<String, Object> input) throws Exception;
    int ifina2100U0_U0(Map<String, Object> input) throws Exception;
    int ifina2100D0_D0(Map<String, Object> input) throws Exception;
}
