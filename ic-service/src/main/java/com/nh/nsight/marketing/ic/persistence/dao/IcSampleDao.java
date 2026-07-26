package com.nh.nsight.marketing.ic.persistence.dao;

import com.nh.nsight.marketing.ic.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ic.persistence.dto.sample.SampleRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class IcSampleDao {

    public SampleRow selectSample(SampleSearchCriteria criteria) {
        SampleRow row = new SampleRow();
        row.setSampleKey(criteria.getSampleKey());
        row.setSampleName("IntegrationCustomer sample response");
        row.setDatabase("RDW/ADW mapper hook");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
