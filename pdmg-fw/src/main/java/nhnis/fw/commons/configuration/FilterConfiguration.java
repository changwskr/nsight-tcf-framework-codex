package nhnis.fw.commons.configuration;

import java.util.EnumSet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.DispatcherType;
import nhnis.fw.commons.filter.DefaultFilter;

/**
 * 레거시 DefaultFilter 등록.
 *
 * <p>{@code @Component} Filter 의 자동 등록과 중복되지 않도록
 * 이 {@link FilterRegistrationBean} 만으로 서블릿에 매핑한다.
 */
@Configuration
@ConditionalOnProperty(name = "nhnis.fw.commons.filter.enabled", havingValue = "true")
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<DefaultFilter> defaultFilterRegistration(DefaultFilter defaultFilter) {
        FilterRegistrationBean<DefaultFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(defaultFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registrationBean.setName("pdmgDefaultFilter");
        return registrationBean;
    }
}
