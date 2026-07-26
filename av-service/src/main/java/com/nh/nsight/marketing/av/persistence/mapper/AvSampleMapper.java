package com.nh.nsight.marketing.av.persistence.mapper;

import com.nh.nsight.marketing.av.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.av.persistence.dto.sample.SampleRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AvSampleMapper {
    List<SampleRow> searchSamples(SampleSearchCriteria criteria);

    int countSamples(SampleSearchCriteria criteria);
}
