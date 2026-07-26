package com.nh.nsight.marketing.av.persistence.dao;

import com.nh.nsight.marketing.av.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.av.persistence.dto.sample.SampleRow;
import com.nh.nsight.marketing.av.persistence.mapper.AvSampleMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AvSampleDao {
    private final AvSampleMapper mapper;

    public AvSampleDao(AvSampleMapper mapper) {
        this.mapper = mapper;
    }

    public List<SampleRow> searchSamples(SampleSearchCriteria criteria) {
        return mapper.searchSamples(criteria);
    }

    public int countSamples(SampleSearchCriteria criteria) {
        return mapper.countSamples(criteria);
    }
}
