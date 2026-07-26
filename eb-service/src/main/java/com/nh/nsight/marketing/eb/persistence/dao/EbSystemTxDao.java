package com.nh.nsight.marketing.eb.persistence.dao;

import com.nh.nsight.marketing.eb.application.dto.systemtx.SystemTxSearchCriteria;
import com.nh.nsight.marketing.eb.persistence.dto.systemtx.SystemTxRow;
import com.nh.nsight.marketing.eb.persistence.mapper.EbSystemTxMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class EbSystemTxDao {
    private final EbSystemTxMapper mapper;

    public EbSystemTxDao(EbSystemTxMapper mapper) {
        this.mapper = mapper;
    }

    public List<SystemTxRow> search(SystemTxSearchCriteria criteria) {
        return mapper.search(criteria);
    }

    public int count(SystemTxSearchCriteria criteria) {
        return mapper.count(criteria);
    }
}
