package nhnis.mg.jw.a.persistence.dao;

import java.util.Map;

import nhnis.mg.jw.a.config.RDWMapper;

@RDWMapper
public interface mgjwa1004DAO {
    Map<String, Object> selectSecurityPolicy(String policyId);
    int mergeSecurityPolicy(Map<String, Object> params);
}
