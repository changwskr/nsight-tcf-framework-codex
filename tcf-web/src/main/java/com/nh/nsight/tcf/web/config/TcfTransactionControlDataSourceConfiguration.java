package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.web.support.TcfDataSourceUrlSupport;
import com.nh.nsight.tcf.web.support.TcfHikariDataSources;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 거래통제 규칙({@code TCF_TRANSACTION_CONTROL})은 OM과 동일한 nsight_om DB를 참조해야 한다.
 * primary와 URL이 같으면 풀을 재사용하고, 다르면 Spring이 종료 시 close()하는 별도 DataSource 빈을 등록한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "transaction-control-enabled", havingValue = "true", matchIfMissing = true)
public class TcfTransactionControlDataSourceConfiguration {

    @Configuration
    @Conditional(TransactionControlReusesPrimaryCondition.class)
    static class ReusePrimaryTransactionControlConfiguration {

        private static final Logger log = LoggerFactory.getLogger(ReusePrimaryTransactionControlConfiguration.class);

        @Bean(name = "transactionControlJdbcTemplate")
        public JdbcTemplate transactionControlJdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
            log.info("Transaction control reuses primary datasource");
            return new JdbcTemplate(dataSource);
        }
    }

    @Configuration
    @Conditional(TransactionControlUsesSeparateDataSourceCondition.class)
    static class SeparateTransactionControlConfiguration {

        private static final Logger log = LoggerFactory.getLogger(SeparateTransactionControlConfiguration.class);

        @Bean(name = "transactionControlDataSource", destroyMethod = "close")
        public DataSource transactionControlDataSource(TcfProperties properties, Environment environment) {
            String url = TcfDataSourceUrlSupport.resolveTransactionControlUrl(properties, environment);
            log.info("Transaction control datasource url={}", url);
            TcfProperties.TransactionLogDataSource cfg = properties.getTransactionLogDatasource();
            return TcfHikariDataSources.create(
                    cfg.getDriverClassName(),
                    url,
                    cfg.getUsername(),
                    cfg.getPassword(),
                    "nsight-tx-control-hikari");
        }

        @Bean(name = "transactionControlJdbcTemplate")
        public JdbcTemplate transactionControlJdbcTemplate(
                @Qualifier("transactionControlDataSource") DataSource transactionControlDataSource) {
            return new JdbcTemplate(transactionControlDataSource);
        }
    }
}
