package com.nh.nsight.marketing.ms.persistence.dao;

import com.nh.nsight.marketing.ms.application.dto.sample.SampleSearchCriteria;
import com.nh.nsight.marketing.ms.persistence.dto.sample.SampleRow;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class MsSampleDao {
    public SampleRow selectSample(SampleSearchCriteria criteria) {
        SampleRow row = new SampleRow();
        row.setSampleKey(criteria.getSampleKey());
        row.setSampleName("MiniSingleView sample response");
        row.setDatabase("RDW/ADW mapper hook");
        row.setCreatedAt(OffsetDateTime.now().toString());
        return row;
    }
}
