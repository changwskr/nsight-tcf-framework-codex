package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina0200DAO {
    List<Map<String, Object>> ifina0200S0_checklistOpen(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina0200S0_eol(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> ifina0200S0_gateOpen(Map<String, Object> input) throws Exception;
}
