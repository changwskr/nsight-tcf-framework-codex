package nhnis.mg.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PdmgUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdmgUiApplication.class, args);
    }
}
