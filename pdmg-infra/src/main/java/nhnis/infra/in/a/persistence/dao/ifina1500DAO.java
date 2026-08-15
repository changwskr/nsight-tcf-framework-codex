package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina1500DAO {
    List<Map<String, Object>> ifina1500S0_org(Map<String, Object> input) throws Exception;
    int ifina1500S0_org_count(Map<String, Object> input) throws Exception;
    int ifina1500S0_org_exists(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina1500S0_person(Map<String, Object> input) throws Exception;
    int ifina1500S0_person_count(Map<String, Object> input) throws Exception;
    int ifina1500S0_person_exists(Map<String, Object> input) throws Exception;
    String ifina1500S0_roleByPersonId(Map<String, Object> input) throws Exception;
    int ifina1500C0_org(Map<String, Object> input) throws Exception;
    int ifina1500U0_org(Map<String, Object> input) throws Exception;
    int ifina1500C0_person(Map<String, Object> input) throws Exception;
    int ifina1500U0_person(Map<String, Object> input) throws Exception;
    int ifina1500E0_role(Map<String, Object> input) throws Exception;
}
