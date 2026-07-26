package com.nh.nsight.tcf.ui.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.tcf.ui.config.TcfUiProperties;
import com.nh.nsight.tcf.ui.client.TransactionRelayService.RelayOptions;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class JwtJwksRelayService {
    private static final int JWT_LOCAL_PORT = 8110;
    private static final String JWT_CONTEXT = "/jwt";
    private static final String JWKS_PATH = "/.well-known/jwks.json";

    private final TcfUiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public JwtJwksRelayService(TcfUiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String resolveJwksUrl(RelayOptions options) {
        if (resolveMode(options) == TcfUiProperties.DeploymentMode.tomcat) {
            return trimTrailingSlash(resolveTomcatGateway(options)) + JWT_CONTEXT + JWKS_PATH;
        }
        return trimTrailingSlash(resolveBootrunHost(options)) + ":" + JWT_LOCAL_PORT + JWKS_PATH;
    }

    public Map<String, Object> fetch(RelayOptions options) {
        String url = resolveJwksUrl(options);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        try {
            String body = restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(String.class);
            result.put("jwks", objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {}));
            return result;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("JWK 조회 실패 (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (Exception e) {
            throw new IllegalStateException("JWK 조회 실패: " + e.getMessage(), e);
        }
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
        if (!StringUtils.hasText(value)) {
            return "http://127.0.0.1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
