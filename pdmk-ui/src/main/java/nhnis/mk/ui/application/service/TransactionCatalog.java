package nhnis.mk.ui.application.service;

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
import nhnis.mk.ui.support.TransactionInfo;

/**
 * pdmk-service가 제공하는 거래 목록.
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
                "mkcoa8888S0",
                "이미지로그 목록 조회",
                "mkcoa8888",
                "POST",
                "/mkcoa8888S0",
                "TB_FW_IMAGE_LOG 조회(페이징). 관리 화면(/imagelog)에서 사용한다.",
                readSample("mkcoa8888-list.json")));

        register(new TransactionInfo(
                "mkcoa8888D0",
                "이미지로그 삭제",
                "mkcoa8888",
                "POST",
                "/mkcoa8888D0",
                "TB_FW_IMAGE_LOG 삭제. dto.guidList 로 다건 삭제한다.",
                readSample("mkcoa8888-delete.json")));

        register(new TransactionInfo(
                "mkcoa5530S0",
                "안내항목 목록 조회",
                "mkcoa5530",
                "POST",
                "/mkcoa5530S0",
                "TB_MK_CO_A_5530 목록 조회(페이징).",
                readSample("mkcoa5530-list.json")));

        register(new TransactionInfo(
                "mkcoa9999S0",
                "영업팁 실적 목록 조회",
                "mkcoa9999",
                "POST",
                "/mkcoa9999S0",
                "TB_CR_AH_SALES_TIP_RACT 목록 조회. dto.salzTipKdc 를 비우면 전체를 조회한다.",
                readSample("mkcoa9999-list.json")));
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
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("샘플 전문을 읽지 못했습니다: " + fileName, e);
        }
    }
}
