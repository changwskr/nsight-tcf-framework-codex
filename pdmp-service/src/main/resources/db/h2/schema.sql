-- 로컬 개발 PC용 H2 스키마.
-- 사내 폐쇄망에서는 Oracle의 실제 테이블을 사용하므로 spring.sql.init 설정과 함께 비활성화한다.

DROP TABLE IF EXISTS TB_CR_AH_SALES_TIP_RACT;

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
