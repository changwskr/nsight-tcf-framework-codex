package nhnis.fw.commons.fos;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import nhnis.fw.commons.context.SpringContext;
import nhnis.fw.commons.fos.enums.NameSpace;
import nhnis.fw.commons.fos.enums.Tenant;

@Component
public class ObjectStorageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStorageHandler.class);

    @Value("${storage.fos.connect-timeout:-1}")
    private int connectTimeout;

    @Value("${storage.fos.response-timeout:-1}")
    private int responseTimeout;

    private static final String DOT = ".";
    private static final String SLASH = "/";
    private static final String AUTHORIZATION = "Authorization";

    public ObjectStorageDto put(
            Tenant tenant,
            NameSpace nameSpace,
            String filePath,
            InputStream inputStream,
            ContentType contentType
    ) throws IOException {
        String url = getAddress(tenant, nameSpace, filePath);
        HttpPut request = new HttpPut(url);
        setAuthToken(nameSpace, tenant, (ClassicHttpRequest) request);

        if (contentType == null) {
            contentType = ContentType.APPLICATION_OCTET_STREAM;
        }

        request.setEntity(new InputStreamEntity(inputStream, contentType));
        RequestConfig requestConfig = getRequestConfig();
        request.setConfig(requestConfig);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpClientResponseHandler<ObjectStorageDto> responseHandler = response -> {
                int statusCode = response.getCode();

                if (statusCode >= 300) {
                    String body = response.getEntity() == null
                            ? ""
                            : EntityUtils.toString(response.getEntity());
                    throw new IOException(
                            "(PUT) Failed. status=" + statusCode + " body=" + body
                    );
                }

                ObjectStorageDto dto = new ObjectStorageDto();

                if (response.getEntity() != null) {
                    dto.setHttpStatusCode(statusCode);
                    dto.setSuccess(true);
                }

                return dto;
            };

            return client.execute(request, responseHandler);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    public ObjectStorageDto get(
            Tenant tenant,
            NameSpace nameSpace,
            String filePath
    ) throws IOException {
        String url = getAddress(tenant, nameSpace, filePath);
        HttpGet request = new HttpGet(url);
        setAuthToken(nameSpace, tenant, (ClassicHttpRequest) request);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpClientResponseHandler<ObjectStorageDto> responseHandler = response -> {
                int statusCode = response.getCode();

                if (statusCode >= 300) {
                    String body = response.getEntity() == null
                            ? ""
                            : EntityUtils.toString(response.getEntity());
                    throw new IOException(
                            "(GET) Failed. status=" + statusCode + " body=" + body
                    );
                }

                ObjectStorageDto dto = new ObjectStorageDto();

                if (response.getEntity() != null) {
                    dto.setInputSteram(response.getEntity().getContent());
                    dto.setHttpStatusCode(statusCode);
                    dto.setSuccess(true);
                }

                return dto;
            };

            return client.execute(request, responseHandler);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    public ObjectStorageDto delete(
            Tenant tenant,
            NameSpace nameSpace,
            String filePath
    ) throws IOException {
        String url = getAddress(tenant, nameSpace, filePath);
        HttpDelete request = new HttpDelete(url);
        setAuthToken(nameSpace, tenant, (ClassicHttpRequest) request);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpClientResponseHandler<ObjectStorageDto> responseHandler = response -> {
                int statusCode = response.getCode();

                if (statusCode >= 300) {
                    String body = response.getEntity() == null
                            ? ""
                            : EntityUtils.toString(response.getEntity());
                    throw new IOException(
                            "(DELETE) Failed. status=" + statusCode + " body=" + body
                    );
                }

                ObjectStorageDto dto = new ObjectStorageDto();

                if (response.getEntity() != null) {
                    dto.setInputSteram(response.getEntity().getContent());
                    dto.setHttpStatusCode(statusCode);
                    dto.setSuccess(true);
                }

                return dto;
            };

            return client.execute(request, responseHandler);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    private RequestConfig getRequestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(
                        (connectTimeout <= 0) ? -1 : (connectTimeout * 1000),
                        TimeUnit.MICROSECONDS
                )
                .setResponseTimeout(
                        (responseTimeout <= 0) ? -1 : (responseTimeout * 1000),
                        TimeUnit.MILLISECONDS
                )
                .build();
    }

    private void setAuthToken(
            NameSpace nameSpace,
            Tenant tenant,
            ClassicHttpRequest request
    ) {
        String token = SpringContext.getProperty(
                "storage.fos.token."
                        + tenant.getTenant()
                        + DOT
                        + nameSpace.getNameSpace()
        );

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[FOS - Token: {}]", token);
        }

        if (StringUtils.hasLength(token)) {
            request.addHeader(AUTHORIZATION, token);
        }
    }

    private String getAddress(
            Tenant tenant,
            NameSpace nameSpace,
            String filePath
    ) {
        String httpProtocol = SpringContext.getProperty(
                "storage.fos.base-http-protocol"
        );
        String domain = SpringContext.getProperty("storage.fos.base-domain");

        return String.valueOf(httpProtocol)
                + "://"
                + nameSpace.getNameSpace()
                + DOT
                + tenant.getTenant()
                + DOT
                + (domain.endsWith(SLASH)
                        ? domain
                        : (String.valueOf(domain) + SLASH))
                + "rest"
                + (filePath.startsWith(SLASH)
                        ? filePath
                        : (SLASH + filePath));
    }
}
