package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina3110DAO {
    List<Map<String, Object>> ifina3110S0_S0(Map<String, Object> input) throws Exception;
    int ifina3110S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina3110S0_S0_exists(Map<String, Object> input) throws Exception;
    int ifina3110C0_C0(Map<String, Object> input) throws Exception;
    int ifina3110U0_U0(Map<String, Object> input) throws Exception;
    int ifina3110D0_D0(Map<String, Object> input) throws Exception;
    int ifina3110S0_S0_countBySystem(Map<String, Object> input) throws Exception;
}
