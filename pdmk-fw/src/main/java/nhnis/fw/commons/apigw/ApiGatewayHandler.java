package nhnis.fw.commons.apigw;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import nhnis.fw.commons.apigw.dto.ApiGatewayDto;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * 이미지 OCR 추출본 (2026-08-04).
 * 원본: ApiGatewayHandler 스크린샷.
 */
public class ApiGatewayHandler {

    private ClientHttpConnector clientHttpConnector;

    @Value("${apigw.connect-timeout:30}")
    private static int CONNECT_TIMEOUT;

    @Value("${apigw.response-timeout:30}")
    private static int RESPONSE_TIMEOUT;

    private static final String SEND_TYPE = "W";

    @Bean
    private ClientHttpConnector connector() {
        if (clientHttpConnector == null) {
            clientHttpConnector = (ClientHttpConnector) new ReactorClientHttpConnector(httpClient());
            return clientHttpConnector;
        } else {
            return clientHttpConnector;
        }
    }

    private HttpClient httpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT * 1000)
                .responseTimeout(Duration.ofMillis(RESPONSE_TIMEOUT * 1000));
    }

    public Object call(ApiGatewayDto dto) {
        Mono<Object> response = WebClient.builder()
                .clientConnector(clientHttpConnector)
                .baseUrl(dto.getUrl())
                .build()
                .post()
                .header(ApiGatewayHeaderType.CONTENT_TYPE.get(), dto.getContentType())
                .header(ApiGatewayHeaderType.INTERFACE_ID.get(), dto.getInterfaceId())
                .header(ApiGatewayHeaderType.RECV_TRX_NAME.get(), dto.getRecvTrxName())
                .header(ApiGatewayHeaderType.RECV_TYPE.get(), dto.getRecvType())
                .header(ApiGatewayHeaderType.REPLY_TYPE.get(), dto.getReplyType())
                .header(ApiGatewayHeaderType.SEND_TYPE.get(), SEND_TYPE)
                .body(BodyInserters.fromValue(dto.getBody()))
                .retrieve()
                .bodyToMono(Object.class);
        Object result = response.block();
        return result;
    }
}
