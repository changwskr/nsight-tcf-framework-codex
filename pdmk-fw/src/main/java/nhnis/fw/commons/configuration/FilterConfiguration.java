package nhnis.fw.commons.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nhnis.fw.commons.filter.DefaultFilter;

/**
 * 레거시 DefaultFilter 등록. TCF 경로와 겹치므로 기본 비활성.
 */
@Configuration
@ConditionalOnProperty(name = "nhnis.fw.commons.filter.enabled", havingValue = "true")
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<DefaultFilter> defaultFilterRefrigration(DefaultFilter defaultFilter) {
        FilterRegistrationBean<DefaultFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(defaultFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
