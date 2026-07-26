package com.nh.nsight.marketing.om.persistence.dao;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class OmSampleDao {
    public Map<String, Object> selectSample(Map<String, Object> body, TransactionContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sampleId", "OM-SAMPLE-0001");
        data.put("sampleName", "Operation Management sample transaction");
        data.put("input", body);
        data.put("note", "실제 업무에서는 이 위치에서 MyBatis Mapper를 호출한다.");
        return data;
    }
}


