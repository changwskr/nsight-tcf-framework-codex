package nhnis.infra.in.a.persistence.dao;

import java.util.List;
import java.util.Map;
import nhnis.infra.in.a.config.RDWMapper;

@RDWMapper
public interface ifina9400DAO {
    List<Map<String, Object>> table1_systemGroups(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table2_groupCapacity(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table3_mwDb(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table4_eol(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table5_ha(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table6_capacity(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table7_strategy(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table8_target(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table9_mapping(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> table10_tco(Map<String, Object> input) throws Exception;
    List<Map<String, Object>> tableInventory(Map<String, Object> input) throws Exception;
}
