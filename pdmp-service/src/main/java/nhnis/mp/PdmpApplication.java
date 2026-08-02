package nhnis.mp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "nhnis")
@ConfigurationPropertiesScan("nhnis")
public class PdmpApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdmpApplication.class, args);
    }
}
