package com.nh.nsight.tcf.core.support.logging;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.util.DateTimeUtil;
import com.nh.nsight.tcf.util.GuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class TransactionLogService {
    private static final Logger log = LoggerFactory.getLogger("transaction.log");

    private final TcfProperties properties;
    private final ObjectProvider<TransactionLogRepository> repositoryProvider;

    public TransactionLogService(TcfProperties properties,
                                 ObjectProvider<TransactionLogRepository> repositoryProvider) {
        this.properties = properties;
        this.repositoryProvider = repositoryProvider;
    }

    public void start(TransactionContext context) {
        StandardHeader h = context.getHeader();
        log.info("TX_START guid={} traceId={} serviceId={} txCode={} userId={} branchId={} channelId={}",
                h.getGuid(), h.getTraceId(), h.getServiceId(), h.getTransactionCode(), h.getUserId(), h.getBranchId(), h.getChannelId());
        context.put("txLogStarted", Boolean.TRUE);
    }

    public void end(TransactionContext context, String resultCode, String errorCode) {
        StandardHeader h = context.getHeader();
        log.info("TX_END guid={} traceId={} serviceId={} resultCode={} errorCode={} elapsedMs={}",
                h.getGuid(), h.getTraceId(), h.getServiceId(), resultCode, errorCode, context.elapsedMillis());
        persist(context, resultCode, errorCode);
    }

    private void persist(TransactionContext context, String resultCode, String errorCode) {
        if (!properties.isTransactionLogEnabled()) {
            return;
        }
        TransactionLogRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            log.warn("TransactionLogRepository not available; skip DB persist. serviceId={}",
                    context.getHeader().getServiceId());
            return;
        }
        try {
            repository.save(toRecord(context, resultCode, errorCode));
            log.info("Transaction log persisted. guid={} serviceId={} resultCode={}",
                    context.getHeader().getGuid(), context.getHeader().getServiceId(), resultCode);
        } catch (Exception e) {
            log.warn("Failed to persist transaction log. guid={} serviceId={}",
                    context.getHeader().getGuid(), context.getHeader().getServiceId(), e);
        }
    }

    private TransactionLogRecord toRecord(TransactionContext context, String resultCode, String errorCode) {
        StandardHeader h = context.getHeader();
        return new TransactionLogRecord(
                GuidGenerator.newGuid(),
                DateTimeUtil.nowKst(),
                h.getBusinessCode(),
                h.getServiceId(),
                h.getTransactionCode(),
                h.getGuid(),
                h.getTraceId(),
                h.getUserId(),
                h.getBranchId(),
                resolveResultStatus(resultCode),
                resultCode,
                errorCode,
                context.elapsedMillis());
    }

    private String resolveResultStatus(String resultCode) {
        if ("S0000".equals(resultCode)) {
            return "SUCCESS";
        }
        return "FAIL";
    }
}
