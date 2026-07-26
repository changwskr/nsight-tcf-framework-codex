package com.nh.nsight.tcf.web.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.timeout.TimeoutContextHolder;
import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicy;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PolicyDrivenQueryTimeoutInterceptorTest {

    @AfterEach
    void tearDown() {
        TimeoutContextHolder.clear();
    }

    @Test
    void copyWithTimeoutCreatesNewMappedStatement() {
        Configuration configuration = new Configuration();
        SqlSource sqlSource = parameterObject -> null;
        MappedStatement source = new MappedStatement.Builder(configuration, "test.select", sqlSource,
                SqlCommandType.SELECT).timeout(3).build();

        MappedStatement copy = MappedStatementSupport.copyWithTimeout(source, 7);

        assertEquals(7, copy.getTimeout());
        assertEquals(3, source.getTimeout());
    }

    @Test
    void interceptorAppliesPolicyTimeout() throws Throwable {
        TcfProperties properties = new TcfProperties();
        properties.setTimeoutPolicyEnabled(true);
        PolicyDrivenQueryTimeoutInterceptor interceptor = new PolicyDrivenQueryTimeoutInterceptor(properties);

        TimeoutPolicy policy = new TimeoutPolicy();
        policy.setDbQueryTimeoutSec(9);
        TimeoutContextHolder.set(policy);

        Configuration configuration = new Configuration();
        SqlSource sqlSource = parameterObject -> null;
        MappedStatement source = new MappedStatement.Builder(configuration, "test.select", sqlSource,
                SqlCommandType.SELECT).build();
        Object[] args = new Object[] {source, new Object()};

        Invocation invocation = new Invocation(null, null, args) {
            @Override
            public Object proceed() {
                MappedStatement applied = (MappedStatement) args[0];
                assertEquals(9, applied.getTimeout());
                return null;
            }
        };

        interceptor.intercept(invocation);
    }
}
