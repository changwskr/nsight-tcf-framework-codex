package nhnis.mk.co.a.dao;

import java.util.List;
import java.util.Map;

import nhnis.mk.config.RDWMapper;

/**
 * 이미지로그 DAO.
 *
 * @logicalName mkcoa7777DAO
 */
@RDWMapper
public interface mkcoa7777DAO {

    /** 이미지로그 목록 조회 (페이징). */
    List<Map<String, Object>> mkcoa7777S0_S0(Map<String, Object> input) throws Exception;

    /** 이미지로그 건수. */
    int mkcoa7777S0_S0_count(Map<String, Object> input) throws Exception;

    /** 이미지로그 삭제 (GUID 목록). */
    int mkcoa7777D0_D0(Map<String, Object> input) throws Exception;
}
