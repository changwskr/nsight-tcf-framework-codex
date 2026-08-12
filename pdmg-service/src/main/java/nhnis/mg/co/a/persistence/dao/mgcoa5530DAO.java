package nhnis.mg.co.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.mg.co.a.config.RDWMapper;

/**
 * 마케팅희망고객(안내항목) DAO.
 *
 * @logicalName mgcoa5530DAO
 */
@RDWMapper
public interface mgcoa5530DAO {

    /** 목록 조회 (페이징). */
    List<Map<String, Object>> mgcoa5530S0_S0(Map<String, Object> input) throws Exception;

    /** 목록 건수. */
    int mgcoa5530S0_S0_count(Map<String, Object> input) throws Exception;
}
