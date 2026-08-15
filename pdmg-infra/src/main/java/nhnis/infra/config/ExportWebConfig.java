package nhnis.infra.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ExportWebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        addDir(registry, "/exports/**", "exports");
        addDir(registry, "/evidence/**", "evidence");
    }

    private static void addDir(ResourceHandlerRegistry registry, String pattern, String folder) {
        Path dir = Paths.get(System.getProperty("user.dir"), "data", folder).toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(pattern).addResourceLocations(location);
    }
}
