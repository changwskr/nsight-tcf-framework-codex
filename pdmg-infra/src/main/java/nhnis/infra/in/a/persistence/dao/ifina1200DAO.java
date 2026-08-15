package nhnis.infra.in.a.persistence.dao;
import java.util.List; import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;
@RDWMapper
public interface ifina1200DAO {
    List<Map<String, Object>> ifina1200S0_tmpl(Map<String, Object> input) throws Exception;
    int ifina1200S0_tmpl_count(Map<String, Object> input) throws Exception;
    int ifina1200S0_tmpl_exists(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina1200S0_item(Map<String, Object> input) throws Exception;
    int ifina1200S0_item_count(Map<String, Object> input) throws Exception;
    int ifina1200S0_item_exists(Map<String, Object> input) throws Exception;
    int ifina1200C0_tmpl(Map<String, Object> input) throws Exception;
    int ifina1200U0_tmpl(Map<String, Object> input) throws Exception;
    int ifina1200D0_tmpl(Map<String, Object> input) throws Exception;
    int ifina1200C0_item(Map<String, Object> input) throws Exception;
    int ifina1200U0_item(Map<String, Object> input) throws Exception;
    int ifina1200D0_item(Map<String, Object> input) throws Exception;
    int ifina1200D0_item_by_tmpl(Map<String, Object> input) throws Exception;
}
