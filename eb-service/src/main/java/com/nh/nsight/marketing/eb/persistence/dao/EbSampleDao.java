package com.nh.nsight.marketing.eb.persistence.dao;

import com.nh.nsight.marketing.eb.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.eb.persistence.dto.sample.SampleRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class EbSampleDao {

    public SampleRow selectSample(SampleSearchCriteria criteria) {
        SampleRow row = new SampleRow();
        row.setSampleKey(criteria.getSampleKey());
        row.setSampleName("Ebm sample response");
        row.setDatabase("RDW/ADW mapper hook");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
