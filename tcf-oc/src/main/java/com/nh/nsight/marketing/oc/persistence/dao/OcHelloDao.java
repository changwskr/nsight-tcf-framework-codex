package com.nh.nsight.marketing.oc.persistence.dao;

import com.nh.nsight.marketing.oc.application.dto.hello.HelloSearchCriteria;
import com.nh.nsight.marketing.oc.persistence.dto.hello.HelloRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class OcHelloDao {

    public HelloRow selectHello(HelloSearchCriteria criteria) {
        HelloRow row = new HelloRow();
        row.setName(criteria.getName());
        row.setMessage("Hello " + criteria.getName());
        row.setModule("tcf-oc");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
