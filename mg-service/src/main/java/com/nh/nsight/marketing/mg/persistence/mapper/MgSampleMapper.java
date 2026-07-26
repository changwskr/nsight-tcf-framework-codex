package com.nh.nsight.marketing.mg.persistence.mapper;

import com.nh.nsight.marketing.mg.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.mg.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MgSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
