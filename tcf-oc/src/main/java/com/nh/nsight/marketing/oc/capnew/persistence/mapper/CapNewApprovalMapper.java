package com.nh.nsight.marketing.oc.capnew.persistence.mapper;

import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewApprovalRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CapNewApprovalMapper {

    List<CapNewApprovalRow> selectAll();

    List<CapNewApprovalRow> selectByScenarioId(@Param("scenarioId") String scenarioId);

    int insert(CapNewApprovalRow row);
}
