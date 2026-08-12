package nhnis.mg.co.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.mg.co.a.config.RDWMapper;

/**
 * 이미지로그 DAO.
 *
 * @logicalName mgcoa8888DAO
 */
@RDWMapper
public interface mgcoa8888DAO {

    /** 이미지로그 목록 조회 (페이징). */
    List<Map<String, Object>> mgcoa8888S0_S0(Map<String, Object> input) throws Exception;

    /** 이미지로그 건수. */
    int mgcoa8888S0_S0_count(Map<String, Object> input) throws Exception;

    /** 이미지로그 삭제 (GUID 목록). */
    int mgcoa8888D0_D0(Map<String, Object> input) throws Exception;
}
