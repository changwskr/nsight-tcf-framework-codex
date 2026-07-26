package com.nh.nsight.marketing.sv.persistence.mapper;

import com.nh.nsight.marketing.sv.application.dto.sample.SampleSearchCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SvSampleCountMapper {

    @Select("""
            <script>
            SELECT COUNT(1)
              FROM SV_SAMPLE
            <where>
              <if test="sampleKey != null and sampleKey != ''">
                AND SAMPLE_KEY LIKE CONCAT('%', #{sampleKey}, '%')
              </if>
            </where>
            </script>
            """)
    int countSamples(SampleSearchCriteria criteria);
}
