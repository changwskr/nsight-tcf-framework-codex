package com.nh.nsight.marketing.pd.persistence.dao;

import com.nh.nsight.marketing.pd.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.pd.persistence.dto.sample.SampleRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class PdSampleDao {
    public SampleRow selectSample(SampleSearchCriteria criteria) {
        SampleRow row = new SampleRow();
        row.setSampleKey(criteria.getSampleKey());
        row.setSampleName("Product sample response");
        row.setDatabase("RDW/ADW mapper hook");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
