package com.nh.nsight.marketing.pc.persistence.mapper;

import com.nh.nsight.marketing.pc.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.pc.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PcSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
