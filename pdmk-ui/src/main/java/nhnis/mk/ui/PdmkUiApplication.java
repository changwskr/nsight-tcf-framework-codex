package nhnis.mk.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PdmkUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdmkUiApplication.class, args);
    }
}
