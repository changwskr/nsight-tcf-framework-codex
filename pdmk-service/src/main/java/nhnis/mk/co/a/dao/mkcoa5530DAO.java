package nhnis.mk.co.a.dao;

import java.util.List;
import java.util.Map;

import nhnis.mk.config.RDWMapper;

/**
 * 마케팅희망고객(안내항목) DAO.
 *
 * @logicalName mkcoa5530DAO
 */
@RDWMapper
public interface mkcoa5530DAO {

    /** 목록 조회 (페이징). */
    List<Map<String, Object>> mkcoa5530S0_S0(Map<String, Object> input) throws Exception;

    /** 목록 건수. */
    int mkcoa5530S0_S0_count(Map<String, Object> input) throws Exception;
}
