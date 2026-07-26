package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.control.TransactionControlRepository;
import com.nh.nsight.tcf.web.persistence.dao.JdbcTransactionControlRepository;
import com.nh.nsight.tcf.web.persistence.dao.TransactionControlSchemaInitializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "transaction-control-enabled", havingValue = "true", matchIfMissing = true)
public class TcfTransactionControlConfiguration {

    @Bean
    public TransactionControlRepository jdbcTransactionControlRepository(
            @Qualifier("transactionControlJdbcTemplate") JdbcTemplate transactionControlJdbcTemplate,
            TcfProperties properties) {
        return new JdbcTransactionControlRepository(transactionControlJdbcTemplate, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "nsight.tcf", name = "transaction-control-schema-auto-init", havingValue = "true", matchIfMissing = true)
    public TransactionControlSchemaInitializer transactionControlSchemaInitializer(
            @Qualifier("transactionControlJdbcTemplate") JdbcTemplate transactionControlJdbcTemplate,
            TcfProperties properties) {
        return new TransactionControlSchemaInitializer(transactionControlJdbcTemplate, properties);
    }
}
