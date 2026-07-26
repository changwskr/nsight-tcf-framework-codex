package com.nh.nsight.marketing.om.support;

import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OmFileDownloadAuditListener {
    private final OmOperationDao dao;

    public OmFileDownloadAuditListener(OmOperationDao dao) {
        this.dao = dao;
    }

    public void recordDownload(String userId, String fileName, long fileSize, String businessCode,
                               boolean success, String clientIp) {
        Map<String, Object> row = new HashMap<>();
        row.put("logId", "FDL-" + UUID.randomUUID());
        row.put("downloadTime", DateTimeUtil.nowKst());
        row.put("userId", userId);
        row.put("fileName", fileName);
        row.put("fileSize", fileSize);
        row.put("businessCode", businessCode);
        row.put("resultStatus", success ? "SUCCESS" : "FAIL");
        row.put("clientIp", clientIp == null ? "127.0.0.1" : clientIp);
        dao.insertFileDownloadLog(row);
    }
}
