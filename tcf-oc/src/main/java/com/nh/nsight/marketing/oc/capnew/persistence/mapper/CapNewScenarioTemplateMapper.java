package com.nh.nsight.marketing.oc.capnew.persistence.mapper;

import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewScenarioTemplateRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CapNewScenarioTemplateMapper {

    List<CapNewScenarioTemplateRow> selectAllEnabled();

    CapNewScenarioTemplateRow selectByCode(@Param("templateCode") String templateCode);

    int insert(CapNewScenarioTemplateRow row);

    int update(CapNewScenarioTemplateRow row);
}
