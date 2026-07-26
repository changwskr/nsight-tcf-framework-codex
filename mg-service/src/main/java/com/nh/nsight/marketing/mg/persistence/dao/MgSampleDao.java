package com.nh.nsight.marketing.mg.persistence.dao;

import com.nh.nsight.marketing.mg.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.mg.persistence.dto.sample.SampleRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class MgSampleDao {
    public SampleRow selectSample(SampleSearchCriteria criteria) {
        SampleRow row = new SampleRow();
        row.setSampleKey(criteria.getSampleKey());
        row.setSampleName("Message sample response");
        row.setDatabase("RDW/ADW mapper hook");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
