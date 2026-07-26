package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.logging.H2DevDataSourceUrls;
import com.nh.nsight.tcf.web.support.TcfHikariDataSources;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "transaction-log-enabled", havingValue = "true", matchIfMissing = true)
public class TcfTransactionLogDataSourceConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "nsight.tcf.transaction-log-datasource", name = "separate", havingValue = "true", matchIfMissing = true)
    static class SeparateTransactionLogDataSourceConfiguration {

        private static final Logger log = LoggerFactory.getLogger(SeparateTransactionLogDataSourceConfiguration.class);

        @Bean(name = "transactionLogDataSource", destroyMethod = "close")
        public DataSource transactionLogDataSource(TcfProperties properties, Environment environment) {
            String url = resolveTransactionLogUrl(environment);
            log.info("Transaction log datasource url={}", url);
            TcfProperties.TransactionLogDataSource cfg = properties.getTransactionLogDatasource();
            return TcfHikariDataSources.create(
                    cfg.getDriverClassName(),
                    url,
                    cfg.getUsername(),
                    cfg.getPassword(),
                    "nsight-txlog-hikari");
        }

        @Bean(name = "transactionLogJdbcTemplate")
        public JdbcTemplate transactionLogJdbcTemplate(
                @Qualifier("transactionLogDataSource") DataSource transactionLogDataSource) {
            return new JdbcTemplate(transactionLogDataSource);
        }

        private static String resolveTransactionLogUrl(Environment environment) {
            String configured = environment.getProperty("nsight.tcf.transaction-log-datasource.url");
            String resolved = H2DevDataSourceUrls.resolveNsightOmUrl(environment, configured);
            if (configured != null && !configured.isBlank()
                    && H2DevDataSourceUrls.isInvalidTcpDatabasePath(
                            environment.resolveRequiredPlaceholders(configured.trim()))) {
                log.warn(
                        "Invalid dev transaction-log TCP URL (use ./nsight_om); was {} — using {}",
                        environment.resolveRequiredPlaceholders(configured.trim()),
                        H2DevDataSourceUrls.DEV_NSIGHT_OM_TCP);
            }
            return resolved;
        }
    }
}
