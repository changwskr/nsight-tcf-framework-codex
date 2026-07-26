package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.web.support.TcfHikariDataSources;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * {@code transactionLogDataSource} 등 사용자 DataSource가 있으면
 * {@link org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration} 이
 * 비활성화되어 {@code dataSource} 빈이 없어질 수 있다. 업무 DB는 명시적으로 등록한다.
 */
@AutoConfiguration(before = { DataSourceAutoConfiguration.class, TcfMyBatisAutoConfiguration.class })
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
@ConditionalOnMissingBean(name = "dataSource")
@EnableConfigurationProperties(DataSourceProperties.class)
public class TcfPrimaryDataSourceAutoConfiguration {

    @Bean(name = "dataSource", destroyMethod = "close")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties properties) {
        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        if (dataSource instanceof HikariDataSource hikari) {
            TcfHikariDataSources.configureForServletContainer(hikari, hikari.getPoolName());
        }
        return dataSource;
    }
}
