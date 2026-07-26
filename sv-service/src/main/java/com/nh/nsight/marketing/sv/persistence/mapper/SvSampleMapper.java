package com.nh.nsight.marketing.sv.persistence.mapper;

import com.nh.nsight.marketing.sv.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.sv.persistence.dto.sample.SampleRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SvSampleMapper {

    List<SampleRow> searchSamples(SampleSearchCriteria criteria);
}
