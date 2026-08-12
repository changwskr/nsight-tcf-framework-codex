package nhnis.mg.jw.a.config;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class MybatisLogInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(MybatisLogInterceptor.class);
    private static final String MDC_SQL_ID = "sqlId";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
        String sqlId = statement.getId();
        MDC.put(MDC_SQL_ID, sqlId);
        long startedAt = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - startedAt;
            if (log.isDebugEnabled()) {
                log.debug("{} | param={} | {}ms", sqlId, parameter, elapsed);
            }
            MDC.remove(MDC_SQL_ID);
        }
    }
}
