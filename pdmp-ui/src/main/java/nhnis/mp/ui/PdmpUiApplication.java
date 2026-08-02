package nhnis.mp.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PdmpUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdmpUiApplication.class, args);
    }
}
