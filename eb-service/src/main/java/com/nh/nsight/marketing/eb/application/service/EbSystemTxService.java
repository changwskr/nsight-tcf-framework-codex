package com.nh.nsight.marketing.eb.application.service;

import com.nh.nsight.marketing.eb.application.dto.systemtx.SystemTxInquiryRequest;
import com.nh.nsight.marketing.eb.application.dto.systemtx.SystemTxInquiryResponse;
import com.nh.nsight.marketing.eb.application.dto.systemtx.SystemTxSearchCriteria;
import com.nh.nsight.marketing.eb.application.rule.EbSystemTxRule;
import com.nh.nsight.marketing.eb.persistence.dao.EbSystemTxDao;
import com.nh.nsight.marketing.eb.persistence.dto.systemtx.SystemTxRow;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EbSystemTxService {
    private final EbSystemTxRule rule;
    private final EbSystemTxDao systemTxDao;

    public EbSystemTxService(EbSystemTxRule rule, EbSystemTxDao systemTxDao) {
        this.rule = rule;
        this.systemTxDao = systemTxDao;
    }

    public SystemTxInquiryResponse inquiry(SystemTxInquiryRequest request, TransactionContext context) {
        rule.validateInquiry(request);
        SystemTxSearchCriteria criteria = rule.buildSearchCriteria(request);
        List<SystemTxRow> rows = systemTxDao.search(criteria);
        int totalCount = systemTxDao.count(criteria);
        return SystemTxInquiryResponse.of(context, criteria, rows, totalCount);
    }
}
