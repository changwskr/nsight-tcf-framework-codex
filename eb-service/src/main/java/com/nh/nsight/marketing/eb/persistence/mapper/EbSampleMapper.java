package com.nh.nsight.marketing.eb.persistence.mapper;

import com.nh.nsight.marketing.eb.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.eb.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EbSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
