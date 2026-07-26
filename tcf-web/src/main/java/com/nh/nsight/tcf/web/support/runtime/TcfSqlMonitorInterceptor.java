package com.nh.nsight.tcf.web.support.runtime;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContextHolder;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.runtime.ActiveTransactionRegistry;
import com.nh.nsight.tcf.core.support.runtime.SlowSqlTracker;
import com.nh.nsight.tcf.core.support.runtime.model.SlowSqlInfo;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        }),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class
        })
})
public class TcfSqlMonitorInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(TcfSqlMonitorInterceptor.class);

    private final TcfProperties properties;
    private final ActiveTransactionRegistry registry;
    private final SlowSqlTracker slowSqlTracker;

    public TcfSqlMonitorInterceptor(
            TcfProperties properties,
            ActiveTransactionRegistry registry,
            SlowSqlTracker slowSqlTracker) {
        this.properties = properties;
        this.registry = registry;
        this.slowSqlTracker = slowSqlTracker;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.isRuntimeMonitorEnabled()) {
            return invocation.proceed();
        }
        Object[] args = invocation.getArgs();
        if (!(args[0] instanceof MappedStatement mappedStatement)) {
            return invocation.proceed();
        }
        String mapperId = mappedStatement.getId();
        String sqlId = resolveSqlId(mapperId);
        String serviceId = resolveServiceId();
        String guid = resolveGuid();
        long start = System.currentTimeMillis();
        registry.markDbWait(guid);
        boolean success = true;
        Object result = null;
        try {
            result = invocation.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            try {
                long end = System.currentTimeMillis();
                long elapsed = end - start;
                registry.updateSqlId(guid, mapperId);
                if (elapsed >= properties.getRuntimeSlowSqlThresholdMs()) {
                    slowSqlTracker.record(new SlowSqlInfo(
                            mapperId,
                            sqlId,
                            serviceId,
                            start,
                            end,
                            elapsed,
                            success,
                            resolveAffectedRows(invocation, result),
                            end));
                }
            } catch (Exception e) {
                log.debug("sql monitor skipped: {}", e.getMessage());
            }
        }
    }

    private static long resolveAffectedRows(Invocation invocation, Object result) {
        if (!"update".equals(invocation.getMethod().getName())) {
            return -1L;
        }
        if (result instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private String resolveServiceId() {
        var context = TransactionContextHolder.get();
        if (context == null || context.getHeader() == null) {
            return null;
        }
        return context.getHeader().getServiceId();
    }

    private String resolveGuid() {
        var context = TransactionContextHolder.get();
        if (context == null || context.getHeader() == null) {
            return null;
        }
        StandardHeader header = context.getHeader();
        return header.getGuid();
    }

    private static String resolveSqlId(String mapperId) {
        if (mapperId == null || mapperId.isBlank()) {
            return null;
        }
        int index = mapperId.lastIndexOf('.');
        return index >= 0 ? mapperId.substring(index + 1) : mapperId;
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
