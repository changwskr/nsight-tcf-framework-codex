package com.nh.nsight.tcf.web.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * DataSource가 2개 이상일 때 MyBatis 자동설정이 비활성화되는 경우,
 * 기본 {@code dataSource}로 SqlSessionFactory를 생성한다.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(SqlSessionFactory.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnMissingBean(SqlSessionFactory.class)
@EnableConfigurationProperties(MybatisProperties.class)
public class TcfMyBatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SqlSessionFactory.class)
    public SqlSessionFactory sqlSessionFactory(
            @Qualifier("dataSource") DataSource dataSource,
            MybatisProperties properties) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        if (properties.getMapperLocations() != null && properties.getMapperLocations().length > 0) {
            factory.setMapperLocations(properties.resolveMapperLocations());
        }
        if (properties.getConfiguration() != null) {
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            properties.getConfiguration().applyTo(configuration);
            factory.setConfiguration(configuration);
        }
        return factory.getObject();
    }

    @Bean
    @ConditionalOnMissingBean(SqlSessionTemplate.class)
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
