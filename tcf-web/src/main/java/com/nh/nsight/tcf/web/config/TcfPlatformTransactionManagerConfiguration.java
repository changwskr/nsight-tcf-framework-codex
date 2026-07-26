package com.nh.nsight.tcf.web.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 다중 DataSource 환경에서 Boot의 {@code DataSourceTransactionManagerAutoConfiguration} 이
 * 비활성화될 때 업무 {@code dataSource} 기준 트랜잭션 매니저를 보장한다.
 */
@AutoConfiguration(after = {
        TcfPrimaryDataSourceAutoConfiguration.class,
        TcfTransactionControlDataSourceConfiguration.class
})
@ConditionalOnClass(PlatformTransactionManager.class)
@ConditionalOnMissingBean(PlatformTransactionManager.class)
@ConditionalOnBean(name = "dataSource")
public class TcfPlatformTransactionManagerConfiguration {

    @Bean
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
