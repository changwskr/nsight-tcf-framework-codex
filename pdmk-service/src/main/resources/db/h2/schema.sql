-- 로컬 개발 PC용 H2 스키마.
-- 사내 폐쇄망에서는 Oracle의 실제 테이블을 사용하므로 spring.sql.init 설정과 함께 비활성화한다.

DROP TABLE IF EXISTS TB_FW_IMAGE_LOG;
DROP TABLE IF EXISTS TB_MK_CO_A_5530;
DROP TABLE IF EXISTS TB_CR_AH_SALES_TIP_RACT;

-- 시스템 전문 헤더 이미지로그 (ImageLogHandler / ImageLogDTO)
CREATE TABLE TB_FW_IMAGE_LOG (
    GUID           VARCHAR(64)   NOT NULL,  -- 거래고유ID (std_gbl_id)
    SERVICE_ID     VARCHAR(50),             -- 서비스ID (rms_svc_c)
    SCREEN_ID      VARCHAR(50),             -- 화면ID (scid)
    OPTR_ENO       VARCHAR(20),             -- 사용자ID (optr_eno)
    CLIENT_IP      VARCHAR(50),             -- 클라이언트IP (tr_trm_ipadr)
    REQUEST_TIME   VARCHAR(17),             -- 요청시간
    RESPONSE_TIME  VARCHAR(17),             -- 응답시간
    EXCEPTION_TYPE VARCHAR(200),            -- 예외타입
    EXCEPTION_CODE VARCHAR(50),             -- 예외코드
    EXCEPTION_MSG  VARCHAR(1000),           -- 예외메시지
    CONSTRAINT TB_FW_IMAGE_LOG_PK PRIMARY KEY (GUID)
);

CREATE TABLE TB_CR_AH_SALES_TIP_RACT (
    TRT_BRC      VARCHAR(5)    NOT NULL,   -- 취급점 코드
    TRTMN_ENO    VARCHAR(10)   NOT NULL,   -- 취급자 사번
    SALZ_TIP_KDC VARCHAR(3)    NOT NULL,   -- 영업팁 종류코드
    BAS_DT       VARCHAR(8)    NOT NULL,   -- 기준일자 (yyyyMMdd)
    PRTO_CN      VARCHAR(4000),            -- 포트폴리오 내용
    INQ_CN       VARCHAR(4000),            -- 조회 내용
    INP_CN       VARCHAR(4000),            -- 입력 내용
    CONSTRAINT TB_CR_AH_SALES_TIP_RACT_PK
        PRIMARY KEY (TRT_BRC, TRTMN_ENO, SALZ_TIP_KDC, BAS_DT)
);

-- mkcoa5530 샘플 (운영 로그 필드 ID L5101~L5104 대응)
CREATE TABLE TB_MK_CO_A_5530 (
    L5101 VARCHAR(20)  NOT NULL,   -- 항목코드
    L5102 VARCHAR(100) NOT NULL,   -- 항목명
    L5103 VARCHAR(8)   NOT NULL,   -- 기준일자
    L5104 VARCHAR(5),              -- 취급점코드
    CONSTRAINT TB_MK_CO_A_5530_PK PRIMARY KEY (L5101, L5103)
);
