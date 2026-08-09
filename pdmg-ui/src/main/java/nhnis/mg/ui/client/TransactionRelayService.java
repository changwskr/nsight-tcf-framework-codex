package nhnis.mg.ui.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import nhnis.mg.ui.application.service.TransactionCatalog;
import nhnis.mg.ui.config.PdmgUiProperties;
import nhnis.mg.ui.support.RelayResult;
import nhnis.mg.ui.support.TransactionInfo;

/**
 * 화면이 보낸 전문을 pdmg-service로 중계한다.
 *
 * <p>브라우저가 pdmg-service를 직접 호출하면 교차 출처가 되어 CORS 설정에 묶이므로,
 * UI 서버가 대신 호출하고 상태·소요시간을 함께 돌려준다.
 */
@Service
public class TransactionRelayService {

    private final TransactionCatalog catalog;
    private final PdmgUiProperties properties;
    private final RestClient restClient;

    public TransactionRelayService(TransactionCatalog catalog, PdmgUiProperties properties) {
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
        return post(transactionId, targetUrl, requestBody);
    }

    /** 카탈로그에 없는 고정 경로(이미지로그 등)를 중계한다. */
    public RelayResult relayPath(String path, String requestBody, String baseUrl) {
        String normalized = path == null ? "/" : path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        String targetUrl = trimTrailingSlash(baseUrl) + normalized;
        return post("path:" + normalized, targetUrl, requestBody);
    }

    private RelayResult post(String id, String targetUrl, String requestBody) {
        long started = System.currentTimeMillis();
        try {
            return restClient.post()
                    .uri(URI.create(targetUrl))
                    .body(requestBody == null ? "{}" : requestBody)
                    .exchange((request, response) -> new RelayResult(
                            id,
                            targetUrl,
                            response.getStatusCode().value(),
                            System.currentTimeMillis() - started,
                            StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)));
        } catch (RestClientResponseException e) {
            return new RelayResult(id, targetUrl, e.getStatusCode().value(),
                    System.currentTimeMillis() - started, e.getResponseBodyAsString());
        } catch (Exception e) {
            return new RelayResult(id, targetUrl, 502,
                    System.currentTimeMillis() - started, connectionErrorJson(targetUrl, e.getMessage()));
        }
    }

    private String connectionErrorJson(String targetUrl, String message) {
        return """
                {"error":"%s","targetUrl":"%s","hint":"pdmg-service가 기동 중인지, 대상 URL이 맞는지 확인하세요."}"""
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
