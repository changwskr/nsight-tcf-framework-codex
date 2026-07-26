package com.nh.nsight.marketing.pc.persistence.dao;

import com.nh.nsight.marketing.pc.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.pc.persistence.dto.sample.SampleRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class PcSampleDao {
    public SampleRow selectSample(SampleSearchCriteria criteria) {
        SampleRow row = new SampleRow();
        row.setSampleKey(criteria.getSampleKey());
        row.setSampleName("PrivateCustomer sample response");
        row.setDatabase("RDW/ADW mapper hook");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
