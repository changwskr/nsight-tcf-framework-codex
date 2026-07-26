package com.nh.nsight.marketing.eb.persistence.mapper;

import com.nh.nsight.marketing.eb.application.dto.systemtx.SystemTxSearchCriteria;
import com.nh.nsight.marketing.eb.persistence.dto.systemtx.SystemTxRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EbSystemTxMapper {

    List<SystemTxRow> search(SystemTxSearchCriteria criteria);

    int count(SystemTxSearchCriteria criteria);
}
