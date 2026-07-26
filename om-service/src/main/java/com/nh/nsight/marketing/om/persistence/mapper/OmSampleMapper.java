package com.nh.nsight.marketing.om.persistence.mapper;

import com.nh.nsight.marketing.om.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.om.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OmSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
