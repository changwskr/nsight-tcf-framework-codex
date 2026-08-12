package nhnis.mg.jw.a.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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
        if (origins == null || origins.isEmpty()) {
            return;
        }
        List<String> methods = corsProperties.getAllowedMethods();
        List<String> headers = corsProperties.getAllowedHeaders();
        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods(methods == null || methods.isEmpty()
                        ? new String[] {"*"}
                        : methods.toArray(String[]::new))
                .allowedHeaders(headers == null || headers.isEmpty()
                        ? new String[] {"*"}
                        : headers.toArray(String[]::new))
                .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            return source;
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        List<String> methods = corsProperties.getAllowedMethods();
        if (methods == null || methods.isEmpty()) {
            config.addAllowedMethod("*");
        } else {
            config.setAllowedMethods(methods);
        }
        List<String> headers = corsProperties.getAllowedHeaders();
        if (headers == null || headers.isEmpty()) {
            config.addAllowedHeader("*");
        } else {
            config.setAllowedHeaders(headers);
        }
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
