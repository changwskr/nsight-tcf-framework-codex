package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicyRepository;
import com.nh.nsight.tcf.web.persistence.dao.JdbcTimeoutPolicyRepository;
import com.nh.nsight.tcf.web.persistence.dao.TimeoutPolicySchemaInitializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "timeout-policy-enabled", havingValue = "true", matchIfMissing = true)
public class TcfTimeoutPolicyConfiguration {

    @Bean
    public TimeoutPolicyRepository jdbcTimeoutPolicyRepository(
            @Qualifier("transactionControlJdbcTemplate") JdbcTemplate transactionControlJdbcTemplate,
            TcfProperties properties) {
        return new JdbcTimeoutPolicyRepository(transactionControlJdbcTemplate, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "nsight.tcf", name = "timeout-policy-schema-auto-init", havingValue = "true", matchIfMissing = true)
    public TimeoutPolicySchemaInitializer timeoutPolicySchemaInitializer(
            @Qualifier("transactionControlJdbcTemplate") JdbcTemplate transactionControlJdbcTemplate,
            TcfProperties properties) {
        return new TimeoutPolicySchemaInitializer(transactionControlJdbcTemplate, properties);
    }
}
