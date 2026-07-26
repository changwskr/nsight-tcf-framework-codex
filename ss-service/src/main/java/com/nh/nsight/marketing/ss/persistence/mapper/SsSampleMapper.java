package com.nh.nsight.marketing.ss.persistence.mapper;

import com.nh.nsight.marketing.ss.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ss.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SsSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
