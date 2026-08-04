package nhnis.fw.tcf.dto;

import java.io.Serializable;

/**
 * 표준 요청 전문. JSON 루트는 {@code header} + {@code body}이다.
 *
 * @param <T> 업무 요청 Body 타입. 업무 DTO 또는 {@code Map<String, Object>}
 */
public class StandardRequestDto<T> implements Serializable {

    private StandardHeaderDto header;
    private T body;

    public StandardRequestDto() {
    }

    public StandardRequestDto(StandardHeaderDto header, T body) {
        this.header = header;
        this.body = body;
    }

    public StandardHeaderDto getHeader() { return header; }
    public void setHeader(StandardHeaderDto header) { this.header = header; }
    public T getBody() { return body; }
    public void setBody(T body) { this.body = body; }
}
