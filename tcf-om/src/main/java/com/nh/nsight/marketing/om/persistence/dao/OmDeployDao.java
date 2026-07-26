package com.nh.nsight.marketing.om.persistence.dao;

import com.nh.nsight.marketing.om.persistence.mapper.OmDeployMapper;
import java.sql.Clob;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class OmDeployDao {
    private final OmDeployMapper mapper;

    public OmDeployDao(OmDeployMapper mapper) {
        this.mapper = mapper;
    }

    public int insertDeployRequest(Map<String, Object> row) {
        return mapper.insertDeployRequest(row);
    }

    public int updateDeployRequest(Map<String, Object> row) {
        return mapper.updateDeployRequest(row);
    }

    public Map<String, Object> selectDeployRequestById(String deployRequestId) {
        return normalizeRequestRow(mapper.selectDeployRequestById(deployRequestId));
    }

    public List<Map<String, Object>> searchDeployRequests(Map<String, Object> criteria) {
        return mapper.searchDeployRequests(criteria).stream().map(this::normalizeRequestRow).toList();
    }

    public int countDeployRequests(Map<String, Object> criteria) {
        return mapper.countDeployRequests(criteria);
    }

    public int insertDeployHistory(Map<String, Object> row) {
        return mapper.insertDeployHistory(row);
    }

    public List<Map<String, Object>> searchDeployHistories(Map<String, Object> criteria) {
        return mapper.searchDeployHistories(criteria);
    }

    public int countDeployHistories(Map<String, Object> criteria) {
        return mapper.countDeployHistories(criteria);
    }

    public Map<String, Object> selectDeployStatusByBusinessCode(String businessCode) {
        return mapper.selectDeployStatusByBusinessCode(businessCode);
    }

    public int deleteAllDeployHistories() {
        return mapper.deleteAllDeployHistories();
    }

    public int deleteAllDeployRequests() {
        return mapper.deleteAllDeployRequests();
    }

    private Map<String, Object> normalizeRequestRow(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Object message = row.get("errorMessage");
        if (message instanceof Clob clob) {
            row.put("errorMessage", readClob(clob));
        } else if (message != null && !(message instanceof String)) {
            row.put("errorMessage", String.valueOf(message));
        }
        return row;
    }

    private static String readClob(Clob clob) {
        try {
            long length = clob.length();
            int size = length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) length;
            return clob.getSubString(1, size);
        } catch (Exception e) {
            return "";
        }
    }
}
