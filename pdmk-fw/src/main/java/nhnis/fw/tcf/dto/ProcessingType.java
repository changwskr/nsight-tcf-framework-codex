package nhnis.fw.tcf.dto;

/**
 * 전문 처리 유형. Header에는 enum 이름 문자열로 담긴다.
 */
public enum ProcessingType {

    /** 조회 */
    INQUIRY,

    /** 등록 */
    CREATE,

    /** 수정 */
    UPDATE,

    /** 삭제 */
    DELETE,

    /** 실행 */
    EXECUTE,

    /** 다운로드 */
    DOWNLOAD,

    /** 업로드 */
    UPLOAD
}
