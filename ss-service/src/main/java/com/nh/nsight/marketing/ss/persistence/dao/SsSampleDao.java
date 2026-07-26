package com.nh.nsight.marketing.ss.persistence.dao;

import com.nh.nsight.marketing.ss.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ss.persistence.dto.sample.SampleRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class SsSampleDao {
    public SampleRow selectSample(SampleSearchCriteria criteria) {
        SampleRow row = new SampleRow();
        row.setSampleKey(criteria.getSampleKey());
        row.setSampleName("SalesSupport sample response");
        row.setDatabase("RDW/ADW mapper hook");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
