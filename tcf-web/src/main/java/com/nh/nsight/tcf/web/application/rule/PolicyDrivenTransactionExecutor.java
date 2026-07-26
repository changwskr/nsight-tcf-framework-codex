package com.nh.nsight.tcf.web.application.rule;

import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicy;
import com.nh.nsight.tcf.core.support.timeout.TimeoutContextHolder;
import com.nh.nsight.tcf.core.support.timeout.TcfServiceTimeoutConstants;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 프로그램적 트랜잭션용. Facade는 {@link PolicyDrivenTransactionAttributeSource} AOP가
 * {@code @Transactional} timeout을 정책값으로 덮어쓴다.
 */
@Component
@ConditionalOnBean(PlatformTransactionManager.class)
public class PolicyDrivenTransactionExecutor {

    private final PlatformTransactionManager transactionManager;

    public PolicyDrivenTransactionExecutor(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public <T> T execute(Supplier<T> action) {
        return execute(TimeoutContextHolder.get(), action);
    }

    public <T> T execute(TimeoutPolicy policy, Supplier<T> action) {
        int timeoutSec = policy != null && policy.getTxTimeoutSec() > 0
                ? policy.getTxTimeoutSec()
                : TcfServiceTimeoutConstants.DEFAULT_TX_TIMEOUT_SEC;
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setTimeout(timeoutSec);
        return template.execute(status -> action.get());
    }
}
