package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.web.application.rule.PolicyDrivenTransactionAttributeSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;

/**
 * {@code transactionAttributeSource} 빈을 직접 등록하면
 * {@link org.springframework.transaction.annotation.ProxyTransactionManagementConfiguration} 과
 * 이름 충돌이 난다. TX Advisor 초기화 후 AttributeSource만 교체한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "timeout-policy-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(PlatformTransactionManager.class)
public class TcfTimeoutTransactionManagementConfiguration {

    private final TcfProperties properties;

    public TcfTimeoutTransactionManagementConfiguration(TcfProperties properties) {
        this.properties = properties;
    }

    @Bean
    BeanPostProcessor policyDrivenTransactionAttributeSourceCustomizer() {
        TcfProperties props = this.properties;
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof BeanFactoryTransactionAttributeSourceAdvisor advisor) {
                    advisor.setTransactionAttributeSource(new PolicyDrivenTransactionAttributeSource(props));
                }
                return bean;
            }
        };
    }
}
