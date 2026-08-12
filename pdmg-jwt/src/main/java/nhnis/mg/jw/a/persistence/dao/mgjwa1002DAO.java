package nhnis.mg.jw.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.mg.jw.a.config.RDWMapper;

@RDWMapper
public interface mgjwa1002DAO {
    List<Map<String, Object>> searchLoginHistories(Map<String, Object> criteria);
    int countLoginHistories(Map<String, Object> criteria);
    int insertLoginHistory(Map<String, Object> params);
}
