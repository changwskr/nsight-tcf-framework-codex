package com.nh.nsight.gateway.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * sessionDataSource 등 추가 DataSource 빈이 있으면 Spring Boot DataSource 자동구성이 비활성화됩니다.
 * 라우팅·TCF_USER_SESSION용 primary DataSource를 명시합니다.
 */
@Configuration
public class GatewayRouteDataSourceConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource gatewayRouteDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public JdbcTemplate gatewayJdbcTemplate(DataSource gatewayRouteDataSource) {
        return new JdbcTemplate(gatewayRouteDataSource);
    }
}
