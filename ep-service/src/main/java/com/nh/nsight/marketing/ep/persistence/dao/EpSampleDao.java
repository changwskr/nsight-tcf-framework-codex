package com.nh.nsight.marketing.ep.persistence.dao;

import com.nh.nsight.marketing.ep.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ep.persistence.dto.sample.SampleRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class EpSampleDao {

    public SampleRow selectSample(SampleSearchCriteria criteria) {
        SampleRow row = new SampleRow();
        row.setSampleKey(criteria.getSampleKey());
        row.setSampleName("EventProcessing sample response");
        row.setDatabase("RDW/ADW mapper hook");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
