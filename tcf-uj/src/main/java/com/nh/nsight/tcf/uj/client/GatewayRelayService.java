package com.nh.nsight.tcf.uj.client;

import com.nh.nsight.tcf.uj.config.TcfUjProperties;
import com.nh.nsight.tcf.uj.support.RelayResult;
import com.nh.nsight.tcf.uj.client.TransactionRelayService.RelayOptions;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GatewayRelayService {
    private static final int GATEWAY_LOCAL_PORT = 8100;
    private static final String GATEWAY_CONTEXT = "/gw";

    private final TcfUjProperties properties;
    private final RestClient restClient;

    public GatewayRelayService(TcfUjProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }

    public String resolveGatewayOnlineUrl(String businessCode, RelayOptions options) {
        String code = businessCode.toLowerCase(Locale.ROOT);
        if (resolveMode(options) == TcfUjProperties.DeploymentMode.tomcat) {
            return trimTrailingSlash(resolveTomcatGateway(options)) + GATEWAY_CONTEXT + "/" + code + "/online";
        }
        return trimTrailingSlash(resolveBootrunHost(options)) + ":" + GATEWAY_LOCAL_PORT + "/" + code + "/online";
    }

    public String resolveGatewayOmUrl(RelayOptions options) {
        return resolveGatewayOnlineUrl("OM", options);
    }

    public RelayResult relayOnline(String businessCode,
                                   String requestBody,
                                   RelayOptions options,
                                   String cookieHeader,
                                   String authorizationHeader) {
        String targetUrl = resolveGatewayOnlineUrl(businessCode, options);
        long started = System.currentTimeMillis();
        try {
            return restClient.post()
                    .uri(URI.create(targetUrl + "?" + relayQuery(options)))
                    .headers(headers -> {
                        if (StringUtils.hasText(authorizationHeader)) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                        }
                        if (StringUtils.hasText(cookieHeader)) {
                            headers.set(HttpHeaders.COOKIE, cookieHeader);
                        }
                    })
                    .body(requestBody)
                    .exchange((request, response) -> {
                        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        List<String> setCookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
                        return new RelayResult(
                                "GW",
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
                    "GW",
                    targetUrl,
                    e.getStatusCode().value(),
                    System.currentTimeMillis() - started,
                    e.getResponseBodyAsString(),
                    setCookies
            );
        } catch (Exception e) {
            return new RelayResult(
                    "GW",
                    targetUrl,
                    502,
                    System.currentTimeMillis() - started,
                    connectionErrorJson(businessCode, targetUrl, e.getMessage()),
                    List.of()
            );
        }
    }

    public RelayResult relayOm(String requestBody,
                               String authorizationHeader,
                               RelayOptions options,
                               String cookieHeader) {
        return relayOnline("OM", requestBody, options, cookieHeader, authorizationHeader);
    }

    private String relayQuery(RelayOptions options) {
        String mode = resolveMode(options).name();
        String bootrunHost = resolveBootrunHost(options);
        String tomcatGatewayUrl = resolveTomcatGateway(options);
        return "deploymentMode=" + mode
                + "&bootrunHost=" + encode(bootrunHost)
                + "&tomcatGatewayUrl=" + encode(tomcatGatewayUrl);
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private TcfUjProperties.DeploymentMode resolveMode(RelayOptions options) {
        if (options != null && StringUtils.hasText(options.deploymentMode())) {
            return TcfUjProperties.DeploymentMode.valueOf(options.deploymentMode());
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

    private String connectionErrorJson(String businessCode, String targetUrl, String message) {
        return """
                {"error":"%s","targetUrl":"%s","businessCode":"%s","hint":"tcf-gateway(8100 또는 /gw) 기동 상태를 확인하세요."}
                """.formatted(escapeJson(message), escapeJson(targetUrl), businessCode.toUpperCase(Locale.ROOT));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
