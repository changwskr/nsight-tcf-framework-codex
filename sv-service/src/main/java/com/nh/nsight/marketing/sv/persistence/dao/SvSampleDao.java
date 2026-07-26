package com.nh.nsight.marketing.sv.persistence.dao;

import com.nh.nsight.marketing.sv.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.sv.persistence.dto.sample.SampleRow;
import com.nh.nsight.marketing.sv.persistence.mapper.SvSampleCountMapper;
import com.nh.nsight.marketing.sv.persistence.mapper.SvSampleMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SvSampleDao {
    private final SvSampleMapper mapper;
    private final SvSampleCountMapper countMapper;

    public SvSampleDao(SvSampleMapper mapper, SvSampleCountMapper countMapper) {
        this.mapper = mapper;
        this.countMapper = countMapper;
    }

    public List<SampleRow> searchSamples(SampleSearchCriteria criteria) {
        return mapper.searchSamples(criteria);
    }

    public int countSamples(SampleSearchCriteria criteria) {
        return countMapper.countSamples(criteria);
    }
}
