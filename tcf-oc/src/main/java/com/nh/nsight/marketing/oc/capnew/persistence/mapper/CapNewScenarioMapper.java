package com.nh.nsight.marketing.oc.capnew.persistence.mapper;

import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewScenarioRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CapNewScenarioMapper {

    List<CapNewScenarioRow> selectAll();

    CapNewScenarioRow selectById(@Param("scenarioId") String scenarioId);

    int insert(CapNewScenarioRow row);

    int update(CapNewScenarioRow row);

    int deleteById(@Param("scenarioId") String scenarioId);
}
