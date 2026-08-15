package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina4200DAO {
    List<Map<String, Object>> ifina4200S0_S0(Map<String, Object> input) throws Exception;
    int ifina4200S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina4200S0_S0_exists(Map<String, Object> input) throws Exception;
    int ifina4200C0_C0(Map<String, Object> input) throws Exception;
    int ifina4200U0_U0(Map<String, Object> input) throws Exception;
    int ifina4200D0_D0(Map<String, Object> input) throws Exception;
}
