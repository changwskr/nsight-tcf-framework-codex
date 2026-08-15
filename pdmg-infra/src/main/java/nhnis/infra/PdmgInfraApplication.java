package nhnis.infra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * PDMG Infra Boot.
 *
 * <p>TCF/commons는 {@code nhnis.fw}, 업무는 {@code nhnis.infra} 를 스캔한다.
 */
@SpringBootApplication(scanBasePackages = {"nhnis.infra", "nhnis.fw"})
@ConfigurationPropertiesScan({"nhnis.infra", "nhnis.fw"})
public class PdmgInfraApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdmgInfraApplication.class, args);
    }
}
