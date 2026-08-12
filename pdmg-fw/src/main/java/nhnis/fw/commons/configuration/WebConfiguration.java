package nhnis.fw.commons.configuration;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import nhnis.fw.commons.interceptor.ServicePreventionInterceptor;
import nhnis.fw.commons.resolver.RequestBodyArgumentResolver;

/**
 * 레거시 웹 설정. PDMG 트랜잭션 로그의 ServicePreventionInterceptor 경로.
 *
 * <p>기본 활성({@code nhnis.fw.commons.legacy-web.enabled=true}).
 * 끄려면 false로 설정한다.
 */
@Configuration
@ConditionalOnProperty(name = "nhnis.fw.commons.legacy-web.enabled", havingValue = "true", matchIfMissing = true)
public class WebConfiguration implements WebMvcConfigurer {

    private final ServicePreventionInterceptor servicePreventionInterceptor;
    private final RequestBodyArgumentResolver resolver;

    public WebConfiguration(ServicePreventionInterceptor servicePreventionInterceptor,
            RequestBodyArgumentResolver resolver) {
        this.servicePreventionInterceptor = servicePreventionInterceptor;
        this.resolver = resolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(servicePreventionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/error");
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
