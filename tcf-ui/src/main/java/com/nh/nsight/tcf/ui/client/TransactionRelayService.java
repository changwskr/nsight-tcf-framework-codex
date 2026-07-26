package com.nh.nsight.tcf.ui.client;

import com.nh.nsight.tcf.ui.application.service.BusinessModuleCatalog;
import com.nh.nsight.tcf.ui.config.TcfUiProperties;
import com.nh.nsight.tcf.ui.support.BusinessModuleInfo;
import com.nh.nsight.tcf.ui.support.RelayResult;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class TransactionRelayService {
    private final TcfUiProperties properties;
    private final BusinessModuleCatalog catalog;
    private final GatewayRelayService gatewayRelayService;
    private final RestClient restClient;

    public TransactionRelayService(TcfUiProperties properties,
                                   BusinessModuleCatalog catalog,
                                   GatewayRelayService gatewayRelayService) {
        this.properties = properties;
        this.catalog = catalog;
        this.gatewayRelayService = gatewayRelayService;
        this.restClient = RestClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }

    public String resolveTargetUrl(String businessCode, RelayOptions options) {
        if (properties.isGatewayRelayEnabled()) {
            return gatewayRelayService.resolveGatewayOnlineUrl(businessCode, options);
        }
        BusinessModuleInfo module = catalog.findByCode(businessCode);
        String baseUrl = resolveDirectBaseUrl(module, options);
        return baseUrl + module.contextPath() + "/online";
    }

    private String resolveDirectBaseUrl(BusinessModuleInfo module, RelayOptions options) {
        if (resolveMode(options) == TcfUiProperties.DeploymentMode.tomcat) {
            return trimTrailingSlash(resolveTomcatGateway(options));
        }
        return trimTrailingSlash(resolveBootrunHost(options)) + ":" + module.localPort();
    }

    public RelayResult relay(String businessCode, String requestBody, RelayOptions options) {
        return relay(businessCode, requestBody, options, null, null);
    }

    public RelayResult relay(String businessCode, String requestBody, RelayOptions options, String cookieHeader) {
        return relay(businessCode, requestBody, options, cookieHeader, null);
    }

    public RelayResult relay(String businessCode,
                             String requestBody,
                             RelayOptions options,
                             String cookieHeader,
                             String authorizationHeader) {
        if (properties.isGatewayRelayEnabled()) {
            return gatewayRelayService.relayOnline(
                    businessCode, requestBody, options, cookieHeader, authorizationHeader);
        }
        BusinessModuleInfo module = catalog.findByCode(businessCode);
        String targetUrl = resolveTargetUrl(businessCode, options);
        long started = System.currentTimeMillis();
        try {
            return restClient.post()
                    .uri(URI.create(targetUrl))
                    .headers(headers -> {
                        if (StringUtils.hasText(cookieHeader)) {
                            headers.set(HttpHeaders.COOKIE, cookieHeader);
                        }
                    })
                    .body(requestBody)
                    .exchange((request, response) -> {
                        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        List<String> setCookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
                        return new RelayResult(
                                businessCode.toUpperCase(),
                                targetUrl,
                                response.getStatusCode().value(),
                                System.currentTimeMillis() - started,
                                responseBody == null ? "" : responseBody,
                                setCookies
                        );
                    });
        } catch (RestClientResponseException e) {
            List<String> setCookies = e.getResponseHeaders() == null
                    ? List.of()
                    : e.getResponseHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
            return new RelayResult(
                    businessCode.toUpperCase(),
                    targetUrl,
                    e.getStatusCode().value(),
                    System.currentTimeMillis() - started,
                    e.getResponseBodyAsString(),
                    setCookies
            );
        } catch (Exception e) {
            return new RelayResult(
                    businessCode.toUpperCase(),
                    targetUrl,
                    502,
                    System.currentTimeMillis() - started,
                    connectionErrorJson(targetUrl, module.localPort(), e.getMessage()),
                    List.of()
            );
        }
    }

    private String connectionErrorJson(String targetUrl, int localPort, String message) {
        return """
                {"error":"%s","targetUrl":"%s","hint":"대상 WAS(포트 %d)가 기동 중인지 확인하세요."}
                """.formatted(escapeJson(message), escapeJson(targetUrl), localPort);
    }

    private TcfUiProperties.DeploymentMode resolveMode(RelayOptions options) {
        if (options != null && StringUtils.hasText(options.deploymentMode())) {
            return TcfUiProperties.DeploymentMode.valueOf(options.deploymentMode());
        }
        return properties.getDeploymentMode();
    }

    private String resolveTomcatGateway(RelayOptions options) {
        if (options != null && StringUtils.hasText(options.tomcatGatewayUrl())) {
            return options.tomcatGatewayUrl();
        }
        return properties.getTomcatGatewayUrl();
    }

    private String resolveBootrunHost(RelayOptions options) {
        if (options != null && StringUtils.hasText(options.bootrunHost())) {
            return options.bootrunHost();
        }
        return properties.getBootrunHost();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record RelayOptions(String deploymentMode, String bootrunHost, String tomcatGatewayUrl) {
    }
}
