package nhnis.mg.ui.support;

/**
 * 전문 릴레이 결과.
 *
 * @param transactionId 거래 ID
 * @param targetUrl     실제 호출한 URL
 * @param httpStatus    응답 HTTP 상태
 * @param elapsedMs     소요 시간(ms)
 * @param responseBody  응답 본문 원문
 */
public record RelayResult(
        String transactionId,
        String targetUrl,
        int httpStatus,
        long elapsedMs,
        String responseBody) {
}
