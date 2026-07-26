package com.nh.nsight.marketing.oc.capnew.persistence.dao;

import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewScenarioTemplateRow;
import com.nh.nsight.marketing.oc.capnew.persistence.mapper.CapNewScenarioTemplateMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CapNewScenarioTemplateDao {

    private final CapNewScenarioTemplateMapper mapper;

    public CapNewScenarioTemplateDao(CapNewScenarioTemplateMapper mapper) {
        this.mapper = mapper;
    }

    public List<CapNewScenarioTemplateRow> findAllEnabled() {
        return mapper.selectAllEnabled();
    }

    public CapNewScenarioTemplateRow findByCode(String templateCode) {
        return mapper.selectByCode(templateCode);
    }

    public void insert(CapNewScenarioTemplateRow row) {
        mapper.insert(row);
    }

    public void update(CapNewScenarioTemplateRow row) {
        mapper.update(row);
    }
}
