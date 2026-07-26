package com.nh.nsight.tcf.util.tpmutil;

import com.nh.nsight.tcf.util.DateTimeUtil;
import com.nh.nsight.tcf.util.GuidGenerator;
import com.nh.nsight.tcf.util.json.TpcJsonEscapeUtils;
import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;
import com.nh.nsight.tcf.util.string.TcfStringUtils;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * TPM(Test Program Manager) 클라이언트 — 업무 WAS {@code /online} 표준 JSON 전문 호출.
 * <p>
 * {@code tcf-ui} Relay 서비스와 동일한 URL 규칙을 사용합니다.
 * <ul>
 * <li>bootrun: {@code {bootrunHost}:{port}/online}</li>
 * <li>tomcat: {@code {gatewayUrl}/{업무코드 소문자}/online}</li>
 * </ul>
 */
@CopiedFrom(module = "tcf-util", sourceClass = "tpcutil", category = UtilCategory.TPM, nativeUtility = true)
public final class tpcutil implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-util";
    public static final String COPIED_FROM_CLASS = "tpcutil";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Map<String, Integer> LOCAL_PORTS = Map.ofEntries(
            Map.entry("CC", 8081),
            Map.entry("IC", 8082),
            Map.entry("PC", 8083),
            Map.entry("BC", 8084),
            Map.entry("MS", 8085),
            Map.entry("SV", 8086),
            Map.entry("PD", 8087),
            Map.entry("CM", 8088),
            Map.entry("EB", 8089),
            Map.entry("EP", 8090),
            Map.entry("BP", 8091),
            Map.entry("BD", 8092),
            Map.entry("SS", 8093),
            Map.entry("CS", 8094),
            Map.entry("CT", 8095),
            Map.entry("MG", 8096),
            Map.entry("OM", 8097),
            Map.entry("UD", 8097),
            Map.entry("ET", 8098));

    private tpcutil() {
    }

    public enum DeploymentMode {
        bootrun, tomcat
    }

    /**
     * @param bootrunHost     bootRun 기준 호스트 (예: {@code http://127.0.0.1})
     * @param gatewayUrl      Tomcat 게이트웨이 (예: {@code http://localhost:8080})
     * @param businessCode    업무구분코드 (예: {@code BD})
     * @param serviceId       서비스 ID (예: {@code BD.Sample.inquiry}) — 호출 메타정보
     * @param transactionCode 거래코드 (예: {@code BD-INQ-0001}) — 호출 메타정보
     * @param requestUrl      요청 URL (비우면 bootrunHost/gatewayUrl + 업무코드로 자동 조합)
     * @param requestJson     표준 요청 JSON 전문
     * @return 응답 JSON 전문 (HTTP 오류 시에도 본문 반환, 연결 실패 시 오류 JSON)
     */
    public static String execute(
            String bootrunHost,
            String gatewayUrl,
            String businessCode,
            String serviceId,
            String transactionCode,
            String requestUrl,
            String requestJson) {
        return execute(new ExecuteParams(
                bootrunHost,
                gatewayUrl,
                businessCode,
                serviceId,
                transactionCode,
                requestUrl,
                DeploymentMode.bootrun), requestJson);
    }

    public static String execute(ExecuteParams params, String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            throw new IllegalArgumentException("requestJson is required");
        }
        String targetUrl = resolveTargetUrl(params);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .header("X-Service-Id", nullToEmpty(params.serviceId()))
                    .header("X-Transaction-Code", nullToEmpty(params.transactionCode()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            return body == null ? "" : body;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return connectionErrorJson(
                    targetUrl,
                    params.businessCode(),
                    params.serviceId(),
                    params.transactionCode(),
                    e.getMessage());
        }
    }

    /**
     * 요청 URL을 결정합니다. {@code requestUrl}이 있으면 그대로 사용하고,
     * 없으면 {@link DeploymentMode}와 업무코드 포트로 조합합니다.
     */
    public static String resolveTargetUrl(ExecuteParams params) {
        if (params == null) {
            throw new IllegalArgumentException("params is required");
        }
        if (params.requestUrl() != null && !params.requestUrl().isBlank()) {
            return params.requestUrl().trim();
        }
        String code = normalizeBusinessCode(params.businessCode());
        DeploymentMode mode = params.deploymentMode() == null ? DeploymentMode.bootrun : params.deploymentMode();
        if (mode == DeploymentMode.tomcat) {
            return trimTrailingSlash(params.gatewayUrl()) + "/" + code.toLowerCase() + "/online";
        }
        int port = LOCAL_PORTS.getOrDefault(code, 8080);
        return trimTrailingSlash(params.bootrunHost()) + ":" + port + "/online";
    }

    private static String normalizeBusinessCode(String businessCode) {
        if (businessCode == null || businessCode.isBlank()) {
            throw new IllegalArgumentException("businessCode is required when requestUrl is empty");
        }
        return businessCode.trim().toUpperCase();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String nullToEmpty(String value) {
        return TcfStringUtils.nullToEmpty(value);
    }

    private static String connectionErrorJson(
            String targetUrl,
            String businessCode,
            String serviceId,
            String transactionCode,
            String message) {
        return """
                {"error":"%s","targetUrl":"%s","businessCode":"%s","serviceId":"%s","transactionCode":"%s","hint":"대상 WAS가 기동 중인지 확인하세요."}
                """
                .formatted(
                        escapeJson(message),
                        escapeJson(targetUrl),
                        escapeJson(businessCode),
                        escapeJson(serviceId),
                        escapeJson(transactionCode));
    }

    private static String escapeJson(String value) {
        return TpcJsonEscapeUtils.escapeJson(value);
    }

    public record ExecuteParams(
            String bootrunHost,
            String gatewayUrl,
            String businessCode,
            String serviceId,
            String transactionCode,
            String requestUrl,
            DeploymentMode deploymentMode) {
        public ExecuteParams {
            deploymentMode = deploymentMode == null ? DeploymentMode.bootrun : deploymentMode;
        }
    }

    /**
     * BD 샘플({@code BD.Sample.inquiry}) 호출 예제.
     * 
     * <pre>
     *   gradle :tcf-util:runTpcutil
     *   java -cp ... com.nh.nsight.tcf.util.tpmutil.tpcutil [요청전문.json]
     * </pre>
     * 
     * 환경변수: TPC_BOOTRUN_HOST, TPC_GATEWAY_URL, TPC_BUSINESS_CODE, TPC_SERVICE_ID,
     * TPC_TRANSACTION_CODE, TPC_REQUEST_URL, TPC_DEPLOYMENT_MODE (bootrun|tomcat)
     */
    public static void main(String[] args) throws IOException {
        String bootrunHost = env("TPC_BOOTRUN_HOST", "http://127.0.0.1");
        String gatewayUrl = env("TPC_GATEWAY_URL", "http://localhost:8080");
        String businessCode = env("TPC_BUSINESS_CODE", "BD");
        String serviceId = env("TPC_SERVICE_ID", "BD.Sample.inquiry");
        String transactionCode = env("TPC_TRANSACTION_CODE", "BD-INQ-0001");
        String requestUrl = env("TPC_REQUEST_URL", "");
        DeploymentMode mode = "tomcat".equalsIgnoreCase(env("TPC_DEPLOYMENT_MODE", "bootrun"))
                ? DeploymentMode.tomcat
                : DeploymentMode.bootrun;

        ExecuteParams params = new ExecuteParams(
                bootrunHost,
                gatewayUrl,
                businessCode,
                serviceId,
                transactionCode,
                requestUrl,
                mode);

        String requestJson = args.length > 0 && !args[0].isBlank()
                ? Files.readString(Path.of(args[0]))
                : sampleRequestJson(businessCode, serviceId, transactionCode);

        String targetUrl = resolveTargetUrl(params);
        System.out.println("[tpcutil] mode=" + mode);
        System.out.println("[tpcutil] POST " + targetUrl);
        System.out.println("[tpcutil] serviceId=" + serviceId + " transactionCode=" + transactionCode);
        System.out.println("[tpcutil] request=" + requestJson);
        System.out.println("[tpcutil] response=" + execute(params, requestJson));
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String sampleRequestJson(String businessCode, String serviceId, String transactionCode) {
        String guid = GuidGenerator.newGuid();
        String traceId = GuidGenerator.newTraceId();
        String now = DateTimeUtil.nowKst();
        String today = DateTimeUtil.todayKst();
        return """
                {
                  "header": {
                    "systemId": "NSIGHT-MP",
                    "businessCode": "%s",
                    "serviceId": "%s",
                    "transactionCode": "%s",
                    "processingType": "INQUIRY",
                    "guid": "%s",
                    "traceId": "%s",
                    "channelId": "TPM",
                    "userId": "U123456",
                    "branchId": "001234",
                    "centerId": "DC1",
                    "requestTime": "%s",
                    "clientIp": "127.0.0.1"
                  },
                  "body": {
                    "sampleKey": "%s-SAMPLE",
                    "baseDate": "%s"
                  }
                }
                """.formatted(
                businessCode,
                serviceId,
                transactionCode,
                guid,
                traceId,
                now,
                businessCode,
                today);
    }
}
