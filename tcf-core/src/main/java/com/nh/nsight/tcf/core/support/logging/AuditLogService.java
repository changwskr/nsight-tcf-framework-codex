package com.nh.nsight.tcf.core.support.logging;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private static final Logger auditLog = LoggerFactory.getLogger("audit.log");
    private final TcfProperties properties;

    public AuditLogService(TcfProperties properties) {
        this.properties = properties;
    }

    public void audit(TransactionContext context, String resultCode) {
        if (!properties.isAuditEnabled() || context == null) {
            return;
        }
        StandardHeader h = context.getHeader();
        auditLog.info("AUDIT guid={} serviceId={} txCode={} userId={} branchId={} resultCode={}",
                h.getGuid(), h.getServiceId(), h.getTransactionCode(), h.getUserId(), h.getBranchId(), resultCode);
    }
}
