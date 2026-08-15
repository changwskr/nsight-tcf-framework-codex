package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina4100DAO {
    List<Map<String, Object>> ifina4100S0_S0(Map<String, Object> input) throws Exception;
    int ifina4100S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina4100S0_S0_exists(Map<String, Object> input) throws Exception;
    int ifina4100C0_C0(Map<String, Object> input) throws Exception;
    int ifina4100U0_U0(Map<String, Object> input) throws Exception;
    int ifina4100D0_D0(Map<String, Object> input) throws Exception;
}
