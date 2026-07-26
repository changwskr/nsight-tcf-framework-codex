package com.nh.nsight.marketing.ms.persistence.mapper;

import com.nh.nsight.marketing.ms.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ms.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MsSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
