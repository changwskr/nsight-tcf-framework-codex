package com.nh.nsight.marketing.ic.persistence.mapper;

import com.nh.nsight.marketing.ic.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ic.persistence.dto.sample.SampleRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IcSampleMapper {
    SampleRow selectSample(SampleSearchCriteria criteria);
}
