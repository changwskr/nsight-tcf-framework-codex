package com.nh.nsight.marketing.pd.persistence.mapper;

import com.nh.nsight.marketing.pd.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.pd.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PdSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
