package com.nh.nsight.tcf.ui.client;

import com.nh.nsight.tcf.ui.application.service.BusinessModuleCatalog;
import com.nh.nsight.tcf.ui.config.TcfUiProperties;
import com.nh.nsight.tcf.ui.support.BusinessModuleInfo;
import com.nh.nsight.tcf.ui.client.TransactionRelayService.RelayOptions;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcRelayService {
    private static final String BUSINESS_CODE = "OC";
    private static final String CAPACITY_API_PREFIX = "/api/oc/capacity";
    private static final String CAP_NEW_API_PREFIX = "/api/oc/cap-new";
    private static final String ENV_API_PREFIX = "/api/oc/env";
    private static final String TOMCAT_CONTEXT = "/oc";

    private final TcfUiProperties properties;
    private final BusinessModuleCatalog catalog;
    private final RestClient restClient;

    public OcRelayService(TcfUiProperties properties, BusinessModuleCatalog catalog) {
        this.properties = properties;
        this.catalog = catalog;
        this.restClient = RestClient.builder()
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }

    public String resolveBaseUrl(RelayOptions options) {
        if (resolveMode(options) == TcfUiProperties.DeploymentMode.tomcat) {
            return trimTrailingSlash(resolveTomcatGateway(options)) + TOMCAT_CONTEXT;
        }
        BusinessModuleInfo module = catalog.findByCode(BUSINESS_CODE);
        return trimTrailingSlash(resolveBootrunHost(options)) + ":" + module.localPort();
    }

    public String relayGet(String path, RelayOptions options) {
        return relayGet(CAPACITY_API_PREFIX, path, options);
    }

    public String relayPost(String path, String requestBody, RelayOptions options) {
        return relayPost(CAPACITY_API_PREFIX, path, requestBody, options);
    }

    public String relayCapNewGet(String path, RelayOptions options) {
        return relayGet(CAP_NEW_API_PREFIX, path, options);
    }

    public String relayCapNewPost(String path, String requestBody, RelayOptions options) {
        return relayPost(CAP_NEW_API_PREFIX, path, requestBody, options);
    }

    public String relayCapNewPut(String path, String requestBody, RelayOptions options) {
        return relayPut(CAP_NEW_API_PREFIX, path, requestBody, options);
    }

    public String relayCapNewDelete(String path, RelayOptions options) {
        return relayDelete(CAP_NEW_API_PREFIX, path, options);
    }

    public byte[] relayCapNewPostBinary(String path, String requestBody, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + CAP_NEW_API_PREFIX + path;
        try {
            return restClient.post()
                    .uri(URI.create(targetUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody == null ? "{}" : requestBody)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsByteArray();
        } catch (Exception e) {
            return connectionErrorJson(targetUrl, e).getBytes(StandardCharsets.UTF_8);
        }
    }

    public String relayEnvGet(String path, RelayOptions options) {
        return relayGet(ENV_API_PREFIX, path, options);
    }

    public String relayEnvPost(String path, String requestBody, RelayOptions options) {
        return relayPost(ENV_API_PREFIX, path, requestBody, options);
    }

    public String relayEnvUpload(MultipartFile[] files, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + ENV_API_PREFIX + "/config-files/upload";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                body.add("files", file.getResource());
            }
        }
        try {
            return restClient.post()
                    .uri(URI.create(targetUrl))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            return connectionErrorJson(targetUrl, e);
        }
    }

    public byte[] relayEnvPostBinary(String path, String requestBody, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + ENV_API_PREFIX + path;
        try {
            return restClient.post()
                    .uri(URI.create(targetUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody == null ? "{}" : requestBody)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsByteArray();
        } catch (Exception e) {
            return connectionErrorJson(targetUrl, e).getBytes(StandardCharsets.UTF_8);
        }
    }

    private String relayGet(String apiPrefix, String path, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + apiPrefix + path;
        try {
            return restClient.get()
                    .uri(URI.create(targetUrl))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            return connectionErrorJson(targetUrl, e);
        }
    }

    private String relayPost(String apiPrefix, String path, String requestBody, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + apiPrefix + path;
        try {
            return restClient.post()
                    .uri(URI.create(targetUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody == null ? "{}" : requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            return connectionErrorJson(targetUrl, e);
        }
    }

    private String relayPut(String apiPrefix, String path, String requestBody, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + apiPrefix + path;
        try {
            return restClient.put()
                    .uri(URI.create(targetUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody == null ? "{}" : requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            return connectionErrorJson(targetUrl, e);
        }
    }

    private String relayDelete(String apiPrefix, String path, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + apiPrefix + path;
        try {
            return restClient.delete()
                    .uri(URI.create(targetUrl))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            return connectionErrorJson(targetUrl, e);
        }
    }

    private String connectionErrorJson(String targetUrl, Exception e) {
        String message = e.getMessage() == null ? "tcf-oc에 연결할 수 없습니다." : e.getMessage();
        String hint = resolveMode(null) == TcfUiProperties.DeploymentMode.tomcat
                ? "Tomcat(8080)에 oc.war(/oc)가 배포되어 있는지 확인하세요."
                : "gradle :tcf-oc:bootRun (포트 8094)을 기동했는지 확인하세요.";
        return """
                {"success":false,"message":"%s","data":null,"targetUrl":"%s","hint":"%s"}
                """.formatted(escapeJson(message), escapeJson(targetUrl), escapeJson(hint));
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

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

