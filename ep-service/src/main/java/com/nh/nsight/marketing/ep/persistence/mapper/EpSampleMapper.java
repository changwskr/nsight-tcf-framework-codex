package com.nh.nsight.marketing.ep.persistence.mapper;

import com.nh.nsight.marketing.ep.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ep.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EpSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
