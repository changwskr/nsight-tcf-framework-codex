package nhnis.fw.commons.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import reactor.netty.http.client.HttpClient;

/**
 * DefaultFilter / ResponseBodyArgumentResolver 가 필요로 하는 ClientHttpConnector.
 *
 * <p>WebFlux auto-config가 없을 때를 대비한 보강 빈이다.
 */
@Configuration
@ConditionalOnProperty(name = "nhnis.fw.commons.legacy-web.enabled", havingValue = "true", matchIfMissing = true)
public class ClientHttpConnectorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ClientHttpConnector.class)
    public ClientHttpConnector clientHttpConnector() {
        return new ReactorClientHttpConnector(HttpClient.create());
    }
}
