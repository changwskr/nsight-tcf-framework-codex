package com.nh.nsight.marketing.om.persistence.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OmSampleMapper {
    Map<String, Object> selectSample(Map<String, Object> parameter);
}

