package com.nh.nsight.tcf.web.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class TransactionControlUsesSeparateDataSourceCondition implements Condition {

    private final TransactionControlReusesPrimaryCondition reusesPrimary = new TransactionControlReusesPrimaryCondition();

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !reusesPrimary.matches(context, metadata);
    }
}
