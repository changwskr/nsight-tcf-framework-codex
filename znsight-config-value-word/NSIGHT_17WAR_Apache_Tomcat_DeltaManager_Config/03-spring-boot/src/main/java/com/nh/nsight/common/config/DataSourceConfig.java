package com.nh.nsight.common.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@MapperScan(basePackages = "com.nh.nsight", sqlSessionFactoryRef = "rdwSqlSessionFactory")
public class DataSourceConfig {
  @Bean(name = "rdwDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.rdw")
  public DataSource rdwDataSource() {
    return DataSourceBuilder.create().type(com.zaxxer.hikari.HikariDataSource.class).build();
  }

  @Bean(name = "rdwTransactionManager")
  public PlatformTransactionManager rdwTransactionManager(@Qualifier("rdwDataSource") DataSource ds) {
    DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
    tm.setDefaultTimeout(5);
    return tm;
  }

  @Bean(name = "rdwSqlSessionFactory")
  public SqlSessionFactory rdwSqlSessionFactory(@Qualifier("rdwDataSource") DataSource ds) throws Exception {
    SqlSessionFactoryBean fb = new SqlSessionFactoryBean();
    fb.setDataSource(ds);
    fb.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/**/*.xml"));
    return fb.getObject();
  }
}
