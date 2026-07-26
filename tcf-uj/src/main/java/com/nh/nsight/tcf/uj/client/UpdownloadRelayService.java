package com.nh.nsight.tcf.uj.client;

import com.nh.nsight.tcf.uj.application.service.BusinessModuleCatalog;
import com.nh.nsight.tcf.uj.config.TcfUjProperties;
import com.nh.nsight.tcf.uj.support.BusinessModuleInfo;
import com.nh.nsight.tcf.uj.client.TransactionRelayService.RelayOptions;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class UpdownloadRelayService {
    private static final String BUSINESS_CODE = "UD";

    private final TcfUjProperties properties;
    private final BusinessModuleCatalog catalog;
    private final RestClient restClient;

    public UpdownloadRelayService(TcfUjProperties properties, BusinessModuleCatalog catalog) {
        this.properties = properties;
        this.catalog = catalog;
        this.restClient = RestClient.builder().build();
    }

    public String resolveBaseUrl(RelayOptions options) {
        if (usesTomcatOmEndpoint(options)) {
            return trimTrailingSlash(resolveTomcatGateway(options)) + "/om";
        }
        BusinessModuleInfo module = catalog.findByCode(BUSINESS_CODE);
        return trimTrailingSlash(resolveBootrunHost(options)) + ":" + module.localPort();
    }

    /** OM이 gateway→Tomcat(/om)일 때 updownload도 동일 WAR의 /ud/files 를 사용한다. */
    private boolean usesTomcatOmEndpoint(RelayOptions options) {
        if (properties.isOmGatewayEnabled()) {
            return true;
        }
        return resolveMode(options) == TcfUjProperties.DeploymentMode.tomcat;
    }

    public String relayUpload(MultipartFile file, String userId, String description, String businessCode,
                              RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + "/ud/files/upload";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        if (StringUtils.hasText(userId)) {
            body.add("userId", userId);
        }
        if (StringUtils.hasText(description)) {
            body.add("description", description);
        }
        if (StringUtils.hasText(businessCode)) {
            body.add("businessCode", businessCode);
        }
        return exchangePostMultipart(targetUrl, body);
    }

    public ResponseEntity<ByteArrayResource> relayDownload(String fileId, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + "/ud/files/" + fileId + "/download";
        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(URI.create(targetUrl))
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] content = response.getBody() == null ? new byte[0] : response.getBody();
            return ResponseEntity.status(response.getStatusCode())
                    .headers(headers -> headers.putAll(response.getHeaders()))
                    .body(new ByteArrayResource(content));
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ByteArrayResource(e.getResponseBodyAsByteArray()));
        } catch (Exception e) {
            byte[] body = connectionErrorJson(targetUrl, e).getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ByteArrayResource(body));
        }
    }

    public String relayList(RelayOptions options, Map<String, String> query) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(resolveBaseUrl(options) + "/ud/files");
        if (query != null) {
            query.forEach((key, value) -> {
                if (StringUtils.hasText(value)) {
                    builder.queryParam(key, value);
                }
            });
        }
        URI uri = builder.build().encode(StandardCharsets.UTF_8).toUri();
        return exchangeGet(uri);
    }

    public String relayDetail(String fileId, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + "/ud/files/" + fileId;
        return exchangeGet(targetUrl);
    }

    public String relayUpdate(String fileId, String description, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + "/ud/files/" + fileId;
        Map<String, String> body = new LinkedHashMap<>();
        body.put("description", description == null ? "" : description);
        try {
            return restClient.put()
                    .uri(URI.create(targetUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            return connectionErrorJson(targetUrl, e);
        }
    }

    public String relayDelete(String fileId, RelayOptions options) {
        String targetUrl = resolveBaseUrl(options) + "/ud/files/" + fileId;
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

    private String exchangePostMultipart(String targetUrl, MultiValueMap<String, Object> body) {
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

    private String exchangeGet(URI uri) {
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            return connectionErrorJson(uri.toString(), e);
        }
    }

    private String exchangeGet(String targetUrl) {
        return exchangeGet(URI.create(targetUrl));
    }

    private String connectionErrorJson(String targetUrl, Exception e) {
        String message = e.getMessage() == null ? "대상 서비스에 연결할 수 없습니다." : e.getMessage();
        String hint = usesTomcatOmEndpoint(null)
                ? "Tomcat(8080) /om WAR 배포 및 /om/ud/files API를 확인하세요."
                : "tcf-om(8097)을 기동했는지 확인하세요.";
        return """
                {
                  "header": { "businessCode": "UD" },
                  "body": {
                    "error": "%s",
                    "targetUrl": "%s",
                    "hint": "%s",
                    "files": [],
                    "totalCount": 0
                  }
                }
                """.formatted(escapeJson(message), escapeJson(targetUrl), escapeJson(hint));
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
