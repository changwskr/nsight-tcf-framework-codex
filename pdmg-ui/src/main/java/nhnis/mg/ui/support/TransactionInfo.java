package nhnis.mg.ui.support;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 전문 테스트 대상 거래 한 건.
 *
 * @param id            거래 ID (Controller 메서드명과 동일)
 * @param name          거래명
 * @param programId     전문 ID
 * @param method        HTTP 메서드
 * @param path          pdmg-service 엔드포인트 경로
 * @param description   화면 설명
 * @param sampleRequest 샘플 요청 전문 ({@code {"hdr_nhnis":{...},"dto":{...}}})
 */
public record TransactionInfo(
        String id,
        String name,
        String programId,
        String method,
        String path,
        String description,
        JsonNode sampleRequest) {
}
