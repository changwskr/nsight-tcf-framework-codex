package nhnis.fw.tcf.dto;

import java.io.Serializable;

/**
 * 표준 응답 전문. JSON 루트는 {@code header} + {@code result} + {@code body}이다.
 *
 * <p>실패 시 {@code body}는 null이며, 호출측은 {@code result.resultCode}로 성공을 판별한다.
 *
 * @param <T> 업무 응답 Body 타입
 */
public class StandardResponseDto<T> implements Serializable {

    private StandardHeaderDto header;
    private Result result;
    private T body;

    /**
     * Body만 채운 미완성 응답. header와 result는 ETF가 채운다.
     * 표준 전문 컨트롤러가 업무 결과를 돌려줄 때 쓴다.
     */
    public static <T> StandardResponseDto<T> of(T body) {
        StandardResponseDto<T> response = new StandardResponseDto<>();
        response.body = body;
        return response;
    }

    public static <T> StandardResponseDto<T> success(StandardHeaderDto header, T body) {
        StandardResponseDto<T> response = new StandardResponseDto<>();
        response.header = header;
        response.result = Result.success();
        response.body = body;
        return response;
    }

    public static <T> StandardResponseDto<T> fail(StandardHeaderDto header, String errorCode, String message, String detail) {
        StandardResponseDto<T> response = new StandardResponseDto<>();
        response.header = header;
        response.result = Result.fail(errorCode, message, detail);
        return response;
    }

    public StandardHeaderDto getHeader() { return header; }
    public void setHeader(StandardHeaderDto header) { this.header = header; }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }
    public T getBody() { return body; }
    public void setBody(T body) { this.body = body; }
}
