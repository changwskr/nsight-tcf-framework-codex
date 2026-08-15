package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina5100DAO {
    List<Map<String, Object>> ifina5100S0_S0(Map<String, Object> input) throws Exception;
    int ifina5100S0_S0_count(Map<String, Object> input) throws Exception;
    int ifina5100S0_S0_exists(Map<String, Object> input) throws Exception;
    int ifina5100C0_C0(Map<String, Object> input) throws Exception;
    int ifina5100U0_U0(Map<String, Object> input) throws Exception;
    int ifina5100D0_D0(Map<String, Object> input) throws Exception;
    int ifina5100U0_clearPrimary(Map<String, Object> input) throws Exception;
    int ifina5100S0_dupAddress(Map<String, Object> input) throws Exception;

}
