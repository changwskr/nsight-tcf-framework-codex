package nhnis.mg.co.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.mg.co.a.config.RDWMapper;

/**
 * 거래 파라미터 DAO.
 *
 * @logicalName mgcoa9000DAO
 */
@RDWMapper
public interface mgcoa9000DAO {

    List<Map<String, Object>> mgcoa9000S0_S0(Map<String, Object> input) throws Exception;

    int mgcoa9000S0_S0_count(Map<String, Object> input) throws Exception;

    int mgcoa9000S0_S0_exists(Map<String, Object> input) throws Exception;

    int mgcoa9000C0_C0(Map<String, Object> input) throws Exception;

    int mgcoa9000U0_U0(Map<String, Object> input) throws Exception;

    int mgcoa9000D0_D0(Map<String, Object> input) throws Exception;
}
