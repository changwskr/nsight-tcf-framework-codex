package nhnis.mg.jw.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.mg.jw.a.config.RDWMapper;

@RDWMapper
public interface mgjwa1001DAO {
    List<Map<String, Object>> searchJwtTokens(Map<String, Object> criteria);
    int countJwtTokens(Map<String, Object> criteria);
    Map<String, Object> selectJwtTokenByJti(Map<String, Object> params);
}
