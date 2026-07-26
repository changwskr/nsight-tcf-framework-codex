package com.nh.nsight.tcf.eai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.eai.config.TcfIntegrationProperties;
import com.nh.nsight.tcf.eai.exception.IntegrationErrorCode;
import com.nh.nsight.tcf.eai.exception.IntegrationException;
import com.nh.nsight.tcf.eai.exception.IntegrationTimeoutException;
import com.nh.nsight.tcf.eai.model.IntegrationCallRequest;
import com.nh.nsight.tcf.eai.model.IntegrationCallResult;
import com.nh.nsight.tcf.eai.support.HeaderPropagationHelper;
import com.nh.nsight.tcf.eai.support.ResponseResultValidator;
import com.nh.nsight.tcf.eai.support.StandardRequestBuilder;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * {@link TcfServiceClient} 기본 구현. Spring {@link RestClient} 기반 HTTP/JSON 호출.
 *
 * <p>대상 URL·Timeout 은 {@link TcfIntegrationProperties} (nsight.integration) 에서 조회한다.
 * 표준 오류 변환(Timeout/시스템/전문/업무)과 호출 로그(callerServiceId/targetServiceId/elapsed)를 수행한다.
 */
public class DefaultTcfServiceClient implements TcfServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultTcfServiceClient.class);

    private final TcfIntegrationProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, RestClient> clientCache = new ConcurrentHashMap<>();

    public DefaultTcfServiceClient(TcfIntegrationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntegrationCallResult call(IntegrationCallRequest request, TransactionContext callerContext) {
        String targetBusinessCode = request.getTargetBusinessCode();
        String targetServiceId = request.getTargetServiceId();

        TcfIntegrationProperties.ServiceEndpoint endpoint = properties.findEndpoint(targetBusinessCode);
        if (endpoint == null || endpoint.getBaseUrl() == null || endpoint.getBaseUrl().isBlank()) {
            throw new IntegrationException(IntegrationErrorCode.SYSTEM, targetServiceId,
                    "연동 대상 설정이 없습니다. nsight.integration.services." + targetBusinessCode + " 확인 필요");
        }

        String url = endpoint.resolveOnlineUrl();
        HeaderPropagationHelper.Propagation propagation = HeaderPropagationHelper.from(callerContext);
        Map<String, Object> message = StandardRequestBuilder.build(request, propagation);

        long start = System.currentTimeMillis();
        log.info("[EAI] CALL start caller={} target={} url={} guid={}",
                propagation.getCallerServiceId(), targetServiceId, url, propagation.getGuid());

        try {
            String json = objectMapper.writeValueAsString(message);
            String responseBody = restClient(targetBusinessCode, endpoint)
                    .post()
                    .uri(URI.create(url))
                    .body(json)
                    .retrieve()
                    .body(String.class);

            long elapsed = System.currentTimeMillis() - start;
            IntegrationCallResult result =
                    ResponseResultValidator.parseAndValidate(objectMapper, targetServiceId, responseBody, elapsed);
            log.info("[EAI] CALL end   caller={} target={} resultCode={} elapsedMs={}",
                    propagation.getCallerServiceId(), targetServiceId, result.getResultCode(), elapsed);
            return result;
        } catch (ResourceAccessException e) {
            long elapsed = System.currentTimeMillis() - start;
            if (isTimeout(e)) {
                log.warn("[EAI] CALL timeout target={} url={} elapsedMs={} msg={}",
                        targetServiceId, url, elapsed, e.getMessage());
                throw new IntegrationTimeoutException(targetServiceId,
                        "연동 대상 응답 시간 초과: " + targetServiceId, e);
            }
            log.warn("[EAI] CALL system-error target={} url={} elapsedMs={} msg={}",
                    targetServiceId, url, elapsed, e.getMessage());
            throw new IntegrationException(IntegrationErrorCode.SYSTEM, targetServiceId,
                    "연동 대상 연결 실패: " + targetServiceId, e);
        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[EAI] CALL error target={} url={} elapsedMs={} msg={}",
                    targetServiceId, url, elapsed, e.getMessage());
            throw new IntegrationException(IntegrationErrorCode.SYSTEM, targetServiceId,
                    "연동 호출 실패: " + e.getMessage(), e);
        }
    }

    private boolean isTimeout(ResourceAccessException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException
                    || cause instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private RestClient restClient(String businessCode, TcfIntegrationProperties.ServiceEndpoint endpoint) {
        return clientCache.computeIfAbsent(businessCode.toUpperCase(), key -> buildRestClient(endpoint));
    }

    private RestClient buildRestClient(TcfIntegrationProperties.ServiceEndpoint endpoint) {
        long connectMs = endpoint.getConnectTimeoutMs() != null ? endpoint.getConnectTimeoutMs() : 1000;
        long readMs = endpoint.getReadTimeoutMs() != null
                ? endpoint.getReadTimeoutMs()
                : properties.getDefaultTimeoutMs();

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectMs))
                .withReadTimeout(Duration.ofMillis(readMs));
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }
}
