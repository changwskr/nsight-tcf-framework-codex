package com.nh.nsight.marketing.om.client;

import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OmBatchRemoteClient {
    private final RestTemplate restTemplate;
    private final String batchServiceUrl;

    public OmBatchRemoteClient(RestTemplateBuilder builder,
                               @Value("${nsight.om.batch-service-url:http://127.0.0.1:8098/batch}") String batchServiceUrl) {
        this.batchServiceUrl = batchServiceUrl.endsWith("/")
                ? batchServiceUrl.substring(0, batchServiceUrl.length() - 1)
                : batchServiceUrl;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> runDeployStatusCollect() {
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    batchServiceUrl + "/jobs/deploy-status/run",
                    null,
                    Map.class);
            return response != null ? response : Map.of();
        } catch (RestClientException e) {
            throw new BusinessException("E-OM-BIZ-0003",
                    "tcf-batch 배포 현황 수집 호출 실패: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> runSessionStatusCollect() {
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    batchServiceUrl + "/jobs/session-status/run",
                    null,
                    Map.class);
            return response != null ? response : Map.of();
        } catch (RestClientException e) {
            throw new BusinessException("E-OM-BIZ-0003",
                    "tcf-batch 세션 현황 수집 호출 실패: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> runDbStatusCollect() {
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    batchServiceUrl + "/jobs/db-status/run",
                    null,
                    Map.class);
            return response != null ? response : Map.of();
        } catch (RestClientException e) {
            throw new BusinessException("E-OM-BIZ-0003",
                    "tcf-batch DB 상태 수집 호출 실패: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> runApStatusCollect() {
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    batchServiceUrl + "/jobs/ap-status/run",
                    null,
                    Map.class);
            return response != null ? response : Map.of();
        } catch (RestClientException e) {
            throw new BusinessException("E-OM-BIZ-0003",
                    "tcf-batch AP 상태 수집 호출 실패: " + e.getMessage());
        }
    }

    public Map<String, Object> toExecuteResult(String jobId, Map<String, Object> remote) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배치 재실행");
        result.put("executed", true);
        result.put("jobId", jobId);
        result.put("runStatus", remote.getOrDefault("runStatus", "SUCCESS"));
        result.put("durationMs", remote.get("durationMs"));
        result.put("targetCount", remote.get("targetCount"));
        result.put("successCount", remote.get("successCount"));
        result.put("failCount", remote.get("failCount"));
        result.put("message", remote.get("message"));
        return result;
    }
}
