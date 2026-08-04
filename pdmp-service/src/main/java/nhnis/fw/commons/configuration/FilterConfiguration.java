package nhnis.fw.commons.configuration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nhnis.fw.commons.filter.DefaultFilter;

@Configuration
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
