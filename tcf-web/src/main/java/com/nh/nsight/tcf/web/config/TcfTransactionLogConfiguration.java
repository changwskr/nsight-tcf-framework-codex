package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.logging.TransactionLogRepository;
import com.nh.nsight.tcf.web.persistence.dao.JdbcTransactionLogRepository;
import com.nh.nsight.tcf.web.persistence.dao.TransactionLogSchemaInitializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "transaction-log-enabled", havingValue = "true", matchIfMissing = true)
public class TcfTransactionLogConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "nsight.tcf.transaction-log-datasource", name = "separate", havingValue = "true", matchIfMissing = true)
    static class SeparateDatasourceTransactionLogConfiguration {

        @Bean
        public TransactionLogRepository jdbcTransactionLogRepository(
                @Qualifier("transactionLogJdbcTemplate") JdbcTemplate transactionLogJdbcTemplate,
                TcfProperties properties) {
            return new JdbcTransactionLogRepository(transactionLogJdbcTemplate, properties);
        }

        @Bean
        @ConditionalOnProperty(prefix = "nsight.tcf", name = "transaction-log-schema-auto-init", havingValue = "true", matchIfMissing = true)
        public TransactionLogSchemaInitializer transactionLogSchemaInitializer(
                @Qualifier("transactionLogJdbcTemplate") JdbcTemplate transactionLogJdbcTemplate,
                TcfProperties properties) {
            return new TransactionLogSchemaInitializer(transactionLogJdbcTemplate, properties);
        }
    }

    @Configuration
    @ConditionalOnProperty(prefix = "nsight.tcf.transaction-log-datasource", name = "separate", havingValue = "false")
    static class PrimaryDatasourceTransactionLogConfiguration {

        @Bean
        public TransactionLogRepository jdbcTransactionLogRepository(
                JdbcTemplate jdbcTemplate,
                TcfProperties properties) {
            return new JdbcTransactionLogRepository(jdbcTemplate, properties);
        }

        @Bean
        @ConditionalOnProperty(prefix = "nsight.tcf", name = "transaction-log-schema-auto-init", havingValue = "true", matchIfMissing = true)
        public TransactionLogSchemaInitializer transactionLogSchemaInitializer(
                JdbcTemplate jdbcTemplate,
                TcfProperties properties) {
            return new TransactionLogSchemaInitializer(jdbcTemplate, properties);
        }
    }
}
