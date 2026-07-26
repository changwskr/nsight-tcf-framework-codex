package com.nh.nsight.tcf.core.support.control;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.TcfConsoleLog;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class TransactionControlService {
    private static final Logger log = LoggerFactory.getLogger(TransactionControlService.class);

    private final TcfProperties properties;
    private final TransactionControlValidator validator;
    private final ObjectProvider<TransactionControlRepository> repositoryProvider;

    public TransactionControlService(TcfProperties properties,
                                     TransactionControlValidator validator,
                                     ObjectProvider<TransactionControlRepository> repositoryProvider) {
        this.properties = properties;
        this.validator = validator;
        this.repositoryProvider = repositoryProvider;
    }

    public void check(StandardHeader header) {
        if (!properties.isTransactionControlEnabled()) {
            return;
        }
        if (header != null && TransactionControlExemptions.isExempt(header.getServiceId())) {
            log.debug("Transaction control skipped for exempt service. serviceId={}", header.getServiceId());
            return;
        }
        TcfConsoleLog.boundary("TransactionControlService", "check", "START");
        TransactionControlHeader controlHeader = TransactionControlHeader.from(header);
        validator.validateRequired(controlHeader);

        TransactionControlRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            log.warn("TransactionControlRepository not available; deny transaction. serviceId={}", header.getServiceId());
            throw new BusinessException(ErrorCode.TXCTRL_UNAVAILABLE, "거래통제 조회를 수행할 수 없습니다.");
        }

        Optional<TransactionControlRule> rule = repository.findRule(controlHeader);
        log.info("TX_CTRL_CHECK serviceId={} businessCode={} user={} channelId={} branch={} clientIp={} matched={} controlType={} globalUnblock={}",
                controlHeader.getServiceId(), controlHeader.getBusinessCode(),
                controlHeader.getUser(), controlHeader.getChannelId(), controlHeader.getBranch(),
                controlHeader.getClientIp(),
                rule.isPresent(), rule.map(TransactionControlRule::controlType).orElse("-"),
                repository.isGlobalUnblockActive());

        if (rule.isPresent() && rule.get().isBlocking()) {
            log.info("TX_CTRL_BLOCKED serviceId={} user={} channelId={} branch={} clientIp={} controlType={}",
                    controlHeader.getServiceId(), controlHeader.getUser(),
                    controlHeader.getChannelId(), controlHeader.getBranch(),
                    controlHeader.getClientIp(), rule.get().controlType());
            throw new BusinessException(ErrorCode.TXCTRL_NOT_ALLOWED, "거래통제 규칙에 의해 차단된 거래입니다.");
        }
        if (repository.isGlobalUnblockActive()) {
            log.debug("TX_CTRL_GLOBAL_UNBLOCK serviceId={} — 차단 규칙 미일치, 전체 허용 상태", controlHeader.getServiceId());
        }
        TcfConsoleLog.boundary("TransactionControlService", "check", "END", "allowed");
    }
}
