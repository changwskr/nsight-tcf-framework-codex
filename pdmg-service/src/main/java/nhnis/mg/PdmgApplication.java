package nhnis.mg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "nhnis")
@ConfigurationPropertiesScan("nhnis")
public class PdmgApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdmgApplication.class, args);
    }
}
