package com.nh.nsight.gateway.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(prefix = "nsight.gateway.session-datasource", name = "url")
public class GatewaySessionDataSourceConfiguration {

    @Bean(name = "sessionDataSource")
    public DataSource sessionDataSource(GatewayProperties properties) {
        GatewayProperties.SessionDatasource config = properties.getSessionDatasource();
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(config.getUrl());
        dataSource.setUsername(StringUtils.hasText(config.getUsername()) ? config.getUsername() : "sa");
        dataSource.setPassword(config.getPassword() == null ? "" : config.getPassword());
        if (StringUtils.hasText(config.getDriverClassName())) {
            dataSource.setDriverClassName(config.getDriverClassName());
        }
        dataSource.setPoolName("nsight-gateway-session-hikari");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);
        // SESSIONDB 미기동·일시 장애 시에도 Gateway 기동 (2단계 검증 skip)
        dataSource.setInitializationFailTimeout(-1);
        return dataSource;
    }

    @Bean(name = "sessionJdbcTemplate")
    public JdbcTemplate sessionJdbcTemplate(@Qualifier("sessionDataSource") DataSource sessionDataSource) {
        return new JdbcTemplate(sessionDataSource);
    }
}
