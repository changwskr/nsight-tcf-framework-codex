package nhnis.mk.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebMvcConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins.isEmpty()) {
            return;
        }
        List<String> methods = corsProperties.getAllowedMethods();
        List<String> headers = corsProperties.getAllowedHeaders();
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods(methods.isEmpty() ? new String[]{"*"} : methods.toArray(String[]::new))
                .allowedHeaders(headers.isEmpty() ? new String[]{"*"} : headers.toArray(String[]::new));
    }
}
