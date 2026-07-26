package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.web.support.TcfDataSourceUrlSupport;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class TransactionControlReusesPrimaryCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        TcfProperties properties = bindProperties(context);
        return TcfDataSourceUrlSupport.transactionControlReusesPrimary(properties, context.getEnvironment());
    }

    private static TcfProperties bindProperties(ConditionContext context) {
        TcfProperties properties = new TcfProperties();
        String separate = context.getEnvironment()
                .getProperty("nsight.tcf.transaction-log-datasource.separate", "true");
        properties.getTransactionLogDatasource().setSeparate(Boolean.parseBoolean(separate));
        return properties;
    }
}
