package com.nh.nsight.marketing.om.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OmDeployMapper {
    int insertDeployRequest(Map<String, Object> row);

    int updateDeployRequest(Map<String, Object> row);

    Map<String, Object> selectDeployRequestById(String deployRequestId);

    List<Map<String, Object>> searchDeployRequests(Map<String, Object> criteria);

    int countDeployRequests(Map<String, Object> criteria);

    int insertDeployHistory(Map<String, Object> row);

    List<Map<String, Object>> searchDeployHistories(Map<String, Object> criteria);

    int countDeployHistories(Map<String, Object> criteria);

    Map<String, Object> selectDeployStatusByBusinessCode(String businessCode);

    int deleteAllDeployHistories();

    int deleteAllDeployRequests();
}
