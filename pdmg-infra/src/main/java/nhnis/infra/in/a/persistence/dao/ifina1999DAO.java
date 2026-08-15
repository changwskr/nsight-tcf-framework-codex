package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.infra.in.a.config.RDWMapper;

/**
 * 서버 인벤토리 파일럿 DAO.
 */
@RDWMapper
public interface ifina1999DAO {

    List<Map<String, Object>> ifina1999S0_S0(Map<String, Object> input) throws Exception;

    int ifina1999S0_S0_count(Map<String, Object> input) throws Exception;

    int ifina1999S0_S0_exists(Map<String, Object> input) throws Exception;

    int ifina1999C0_C0(Map<String, Object> input) throws Exception;

    int ifina1999U0_U0(Map<String, Object> input) throws Exception;

    int ifina1999D0_D0(Map<String, Object> input) throws Exception;
}
