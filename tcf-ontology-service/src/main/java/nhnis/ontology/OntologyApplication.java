package nhnis.ontology;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "nhnis.ontology")
@ConfigurationPropertiesScan("nhnis.ontology")
public class OntologyApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OntologyApplication.class);
        if (Arrays.stream(args).anyMatch(a -> a.startsWith("--nhnis.ontology.job="))) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }
        app.run(args);
    }
}
