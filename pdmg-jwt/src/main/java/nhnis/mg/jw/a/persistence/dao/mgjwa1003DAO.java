package nhnis.mg.jw.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.mg.jw.a.config.RDWMapper;

@RDWMapper
public interface mgjwa1003DAO {
    List<Map<String, Object>> searchRefreshTokens(Map<String, Object> criteria);
    int countRefreshTokens(Map<String, Object> criteria);
}
