package com.nh.nsight.marketing.oc.capnew.persistence.dao;

import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewScenarioRow;
import com.nh.nsight.marketing.oc.capnew.persistence.mapper.CapNewScenarioMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CapNewScenarioDao {

    private final CapNewScenarioMapper mapper;

    public CapNewScenarioDao(CapNewScenarioMapper mapper) {
        this.mapper = mapper;
    }

    public List<CapNewScenarioRow> findAll() {
        return mapper.selectAll();
    }

    public CapNewScenarioRow findById(String scenarioId) {
        return mapper.selectById(scenarioId);
    }

    public void insert(CapNewScenarioRow row) {
        mapper.insert(row);
    }

    public void update(CapNewScenarioRow row) {
        mapper.update(row);
    }

    public int deleteById(String scenarioId) {
        return mapper.deleteById(scenarioId);
    }
}
