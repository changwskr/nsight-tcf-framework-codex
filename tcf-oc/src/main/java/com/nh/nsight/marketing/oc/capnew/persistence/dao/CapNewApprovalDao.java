package com.nh.nsight.marketing.oc.capnew.persistence.dao;

import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewApprovalRow;
import com.nh.nsight.marketing.oc.capnew.persistence.mapper.CapNewApprovalMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CapNewApprovalDao {

    private final CapNewApprovalMapper mapper;

    public CapNewApprovalDao(CapNewApprovalMapper mapper) {
        this.mapper = mapper;
    }

    public List<CapNewApprovalRow> findAll() {
        return mapper.selectAll();
    }

    public List<CapNewApprovalRow> findByScenarioId(String scenarioId) {
        return mapper.selectByScenarioId(scenarioId);
    }

    public void insert(CapNewApprovalRow row) {
        mapper.insert(row);
    }
}
