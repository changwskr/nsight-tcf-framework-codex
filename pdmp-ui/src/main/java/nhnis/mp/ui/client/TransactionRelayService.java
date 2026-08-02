package nhnis.mp.ui.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import nhnis.mp.ui.application.service.TransactionCatalog;
import nhnis.mp.ui.config.PdmpUiProperties;
import nhnis.mp.ui.support.RelayResult;
import nhnis.mp.ui.support.TransactionInfo;

/**
 * 화면이 보낸 전문을 pdmp-service로 중계한다.
 *
 * <p>브라우저가 pdmp-service를 직접 호출하면 교차 출처가 되어 CORS 설정에 묶이므로,
 * tcf-ui와 동일하게 UI 서버가 대신 호출하고 상태·소요시간을 함께 돌려준다.
 */
@Service
public class TransactionRelayService {

    private final TransactionCatalog catalog;
    private final PdmpUiProperties properties;
    private final RestClient restClient;

    public TransactionRelayService(TransactionCatalog catalog, PdmpUiProperties properties) {
        this.catalog = catalog;
        this.properties = properties;
        this.restClient = RestClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }

    public String resolveTargetUrl(String transactionId, String baseUrl) {
        TransactionInfo info = catalog.findById(transactionId);
        return trimTrailingSlash(baseUrl) + info.path();
    }

    public RelayResult relay(String transactionId, String requestBody, String baseUrl) {
        String targetUrl = resolveTargetUrl(transactionId, baseUrl);
        long started = System.currentTimeMillis();
        try {
            return restClient.post()
                    .uri(URI.create(targetUrl))
                    .body(requestBody == null ? "{}" : requestBody)
                    .exchange((request, response) -> new RelayResult(
                            transactionId,
                            targetUrl,
                            response.getStatusCode().value(),
                            System.currentTimeMillis() - started,
                            StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)));
        } catch (RestClientResponseException e) {
            return new RelayResult(transactionId, targetUrl, e.getStatusCode().value(),
                    System.currentTimeMillis() - started, e.getResponseBodyAsString());
        } catch (Exception e) {
            return new RelayResult(transactionId, targetUrl, 502,
                    System.currentTimeMillis() - started, connectionErrorJson(targetUrl, e.getMessage()));
        }
    }

    private String connectionErrorJson(String targetUrl, String message) {
        return """
                {"error":"%s","targetUrl":"%s","hint":"pdmp-service가 기동 중인지, 대상 URL이 맞는지 확인하세요."}"""
                .formatted(escapeJson(message), escapeJson(targetUrl));
    }

    private String trimTrailingSlash(String baseUrl) {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : properties.getTargetBaseUrl();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
