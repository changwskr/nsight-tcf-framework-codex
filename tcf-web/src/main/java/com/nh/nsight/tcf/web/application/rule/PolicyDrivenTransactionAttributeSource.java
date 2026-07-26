package com.nh.nsight.tcf.web.application.rule;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.timeout.TimeoutContextHolder;
import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicy;
import java.lang.reflect.Method;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * {@link org.springframework.transaction.annotation.Transactional} 기본 속성은 캐시하고,
 * 호출 시점 {@link TimeoutContextHolder}의 TX timeout으로 덮어쓴다.
 */
public class PolicyDrivenTransactionAttributeSource extends AnnotationTransactionAttributeSource {

    private final TcfProperties properties;

    public PolicyDrivenTransactionAttributeSource(TcfProperties properties) {
        this.properties = properties;
    }

    @Override
    @Nullable
    public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
        TransactionAttribute attr = super.getTransactionAttribute(method, targetClass);
        return applyPolicyTimeout(attr);
    }

    @Nullable
    private TransactionAttribute applyPolicyTimeout(@Nullable TransactionAttribute attr) {
        if (attr == null || !properties.isTimeoutPolicyEnabled()) {
            return attr;
        }
        TimeoutPolicy policy = TimeoutContextHolder.get();
        if (policy == null || policy.getTxTimeoutSec() <= 0) {
            return attr;
        }
        if (!(attr instanceof RuleBasedTransactionAttribute rule)) {
            return attr;
        }
        int policyTimeout = policy.getTxTimeoutSec();
        if (rule.getTimeout() == policyTimeout) {
            return attr;
        }
        return copyWithTimeout(rule, policyTimeout);
    }

    private static RuleBasedTransactionAttribute copyWithTimeout(
            RuleBasedTransactionAttribute source, int timeoutSec) {
        RuleBasedTransactionAttribute copy = new RuleBasedTransactionAttribute();
        copy.setPropagationBehavior(source.getPropagationBehavior());
        copy.setIsolationLevel(source.getIsolationLevel());
        copy.setTimeout(timeoutSec);
        copy.setReadOnly(source.isReadOnly());
        copy.setName(source.getName());
        copy.setQualifier(source.getQualifier());
        copy.getRollbackRules().addAll(source.getRollbackRules());
        return copy;
    }
}
