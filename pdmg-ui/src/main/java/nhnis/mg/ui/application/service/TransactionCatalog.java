package nhnis.mg.ui.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import nhnis.mg.ui.support.TransactionInfo;

/**
 * pdmg-service가 제공하는 거래 목록.
 *
 * <p>요청 Body는 {@code {"hdr_nhnis":{"sys_comm":{...}},"dto":{...}}} 형식이다.
 */
@Service
public class TransactionCatalog {

    private final ObjectMapper objectMapper;
    private final Map<String, TransactionInfo> transactions = new LinkedHashMap<>();

    public TransactionCatalog(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        register(new TransactionInfo(
                "mgcoa8888S0",
                "이미지로그 목록 조회",
                "mgcoa8888",
                "POST",
                "/mgcoa8888S0",
                "pdmg-service TB_FW_IMAGE_LOG 조회(페이징). withinSeconds·minElapsedSeconds·예외여부 지원. 관리 화면(/imagelog)도 동일 API.",
                readSample("mgcoa8888-list.json")));

        register(new TransactionInfo(
                "mgcoa8888D0",
                "이미지로그 삭제",
                "mgcoa8888",
                "POST",
                "/mgcoa8888D0",
                "pdmg-service TB_FW_IMAGE_LOG 삭제. dto.guidList 로 다건 삭제한다.",
                readSample("mgcoa8888-delete.json")));

        register(new TransactionInfo(
                "mgcoa5530S0",
                "마케팅희망고객 목록 조회",
                "mgcoa5530",
                "POST",
                "/mgcoa5530S0",
                "마케팅희망고객/안내항목 목록 조회(페이징).",
                readSample("mgcoa5530-list.json")));

        register(new TransactionInfo(
                "mgcoa9999S0",
                "영업팁 실적 목록 조회",
                "mgcoa9999",
                "POST",
                "/mgcoa9999S0",
                "영업팁 실적 목록 조회. dto.salzTipKdc 를 비우면 전체를 조회한다.",
                readSample("mgcoa9999-list.json")));

        register(new TransactionInfo(
                "mgcoa9000S0",
                "거래 파라미터 목록 조회",
                "mgcoa9000",
                "POST",
                "/mgcoa9000S0",
                "TB_MG_TX_PARAM 조회(페이징). keyword·txId·appId·httpMethod 조건. 관리 화면(/txparam)도 동일 API.",
                readSample("mgcoa9000-list.json")));

        register(new TransactionInfo(
                "mgcoa9000C0",
                "거래 파라미터 등록",
                "mgcoa9000",
                "POST",
                "/mgcoa9000C0",
                "TB_MG_TX_PARAM 등록. txId·txName 필수.",
                readSample("mgcoa9000-create.json")));

        register(new TransactionInfo(
                "mgcoa9000U0",
                "거래 파라미터 수정",
                "mgcoa9000",
                "POST",
                "/mgcoa9000U0",
                "TB_MG_TX_PARAM 수정. txId 기준 거래명·앱·경로·메소드 변경.",
                readSample("mgcoa9000-update.json")));

        register(new TransactionInfo(
                "mgcoa9000D0",
                "거래 파라미터 삭제",
                "mgcoa9000",
                "POST",
                "/mgcoa9000D0",
                "TB_MG_TX_PARAM 삭제. dto.txIdList 로 다건 삭제한다.",
                readSample("mgcoa9000-delete.json")));
    }

    public List<TransactionInfo> findAll() {
        return List.copyOf(transactions.values());
    }

    public TransactionInfo findById(String id) {
        TransactionInfo info = transactions.get(id);
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "등록되지 않은 거래입니다: " + id);
        }
        return info;
    }

    private void register(TransactionInfo info) {
        transactions.put(info.id(), info);
    }

    private JsonNode readSample(String fileName) {
        ClassPathResource resource = new ClassPathResource("sample-requests/" + fileName);
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "샘플 전문이 classpath에 없습니다: sample-requests/" + fileName
                            + " (src/main/resources 확인 후 pdmg-ui 에서 clean processResources 또는 script\\build.bat 실행)");
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("샘플 전문을 읽지 못했습니다: " + fileName, e);
        }
    }
}
