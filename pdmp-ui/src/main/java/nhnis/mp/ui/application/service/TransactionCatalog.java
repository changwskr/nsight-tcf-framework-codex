package nhnis.mp.ui.application.service;

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
import nhnis.mp.ui.support.TransactionInfo;

/**
 * pdmp-service가 제공하는 거래 목록.
 *
 * <p>
 * 샘플 전문은 classpath의 sample-requests/*.json에서 읽는다.
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
                "mpcoa9999S0_S0",
                "영업팁 실적 목록 조회",
                "mpcoa9999",
                "POST",
                "/api/mp/co/a/9999/list",
                "TB_CR_AH_SALES_TIP_RACT 목록 조회. salzTipKdc를 비우면 전체를 조회한다.",
                readSample("mpcoa9999-list.json")));

        register(new TransactionInfo(
                "mpcoa9999S0_S1",
                "영업팁 실적 단건 조회",
                "mpcoa9999",
                "POST",
                "/api/mp/co/a/9999/detail",
                "PK(취급점·취급자·영업팁종류·기준일자) 4개로 단건 조회. 누락 시 FW0001을 반환한다.",
                readSample("mpcoa9999-detail.json")));
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
