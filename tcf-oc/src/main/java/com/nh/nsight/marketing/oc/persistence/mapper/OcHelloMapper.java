package com.nh.nsight.marketing.oc.persistence.mapper;

import com.nh.nsight.marketing.oc.application.dto.hello.HelloSearchCriteria;
import com.nh.nsight.marketing.oc.persistence.dto.hello.HelloRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OcHelloMapper {
    HelloRow selectHello(HelloSearchCriteria criteria);
}
