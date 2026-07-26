package com.nh.nsight.tcf.web.support;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.timeout.TimeoutContextHolder;
import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicy;
import java.util.Properties;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * {@link TimeoutContextHolder}의 {@code dbQueryTimeoutSec}을 Mapper 실행 시 Statement timeout으로 적용한다.
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        }),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class
        })
})
public class PolicyDrivenQueryTimeoutInterceptor implements Interceptor {

    private final TcfProperties properties;

    public PolicyDrivenQueryTimeoutInterceptor(TcfProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.isTimeoutPolicyEnabled()) {
            return invocation.proceed();
        }
        TimeoutPolicy policy = TimeoutContextHolder.get();
        if (policy == null || policy.getDbQueryTimeoutSec() <= 0) {
            return invocation.proceed();
        }
        Object[] args = invocation.getArgs();
        if (!(args[0] instanceof MappedStatement mappedStatement)) {
            return invocation.proceed();
        }
        int policyTimeout = policy.getDbQueryTimeoutSec();
        if (mappedStatement.getTimeout() != null && mappedStatement.getTimeout() == policyTimeout) {
            return invocation.proceed();
        }
        args[0] = MappedStatementSupport.copyWithTimeout(mappedStatement, policyTimeout);
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }
}
