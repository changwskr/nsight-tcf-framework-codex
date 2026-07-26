# 전문 Header 7개 항목 기반 거래통제 설계안

> **구현 상태 (2026-06-28):** `TransactionControlService.check()` + JDBC `findRule` + `TCF_TRANSACTION_CONTROL` 차단 목록 테이블. `BLOCK_YN='Y'` 일치 Row만 차단, 미등록 거래는 기본 허용.

## 1. 적용 범위

Header **7개 항목만**으로 거래를 직접 통제합니다.

| 통제 항목 | 표준 명칭 | DB 컬럼 |
|-----------|-----------|---------|
| Service-id | `serviceId` | `SERVICE_ID` |
| Transaction-code | `transactionCode` | `TRANSACTION_CODE` |
| Business-code | `businessCode` | `BUSINESS_CODE` |
| Service-name | `serviceName` | `SERVICE_NAME` |
| User | `user` | `USER_ID` |
| Channelid | `channelId` | `CHANNEL_ID` |
| Branch | `branch` | `BRANCH_ID` |

**추가 통제 컬럼**

| 항목 | DB 컬럼 | 설명 |
|------|---------|------|
| 통제유형 | `CONTROL_TYPE` | FULL, USER, SERVICE, CHANNEL, BRANCH (운영 분류) |
| 차단여부 | `BLOCK_YN` | Y=차단, N=해제(규칙만 유지) |
| 전체 해제 | `CONTROL_TYPE=GLOBAL` + 7필드 `*` + `BLOCK_YN=N` | 개별 차단 규칙보다 우선, 전 거래 허용 |

## 2. 핵심 개념

> **GLOBAL + BLOCK_YN=N Row가 있으면 전체 차단해제(최우선). 그 외 7필드 일치 + BLOCK_YN=Y이면 차단.**

```text
요청 → GLOBAL 차단해제 Row 존재? → Y → 허용(종료)
     → 아니오 → 7필드 일치 + BLOCK_YN=Y? → Y → 차단
              → 아니오 → 허용
```

## 3. 처리 흐름

```text
OnlineTransactionController → TCF.process() → STF.preProcess()
  → Header 7필드 필수값 검증
  → TCF_TRANSACTION_CONTROL 조회 (CONTROL_TYPE, BLOCK_YN)
  → BLOCK_YN=Y 이면 차단, 아니면 Dispatcher → Handler
```

## 4. 거래통제 테이블 (H2 / Oracle 공통 논리)

```sql
CREATE TABLE TCF_TRANSACTION_CONTROL (
    SERVICE_ID        VARCHAR(100) NOT NULL,
    TRANSACTION_CODE  VARCHAR(50)  NOT NULL,
    BUSINESS_CODE     VARCHAR(10)  NOT NULL,
    SERVICE_NAME      VARCHAR(200) NOT NULL,
    USER_ID           VARCHAR(50)  NOT NULL,
    CHANNEL_ID        VARCHAR(30)  NOT NULL,
    BRANCH_ID         VARCHAR(30)  NOT NULL,
    CONTROL_TYPE      VARCHAR(20)  NOT NULL DEFAULT 'FULL',
    BLOCK_YN          CHAR(1)      NOT NULL DEFAULT 'Y',
    PRIMARY KEY (SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE,
                 SERVICE_NAME, USER_ID, CHANNEL_ID, BRANCH_ID)
);
```

## 5. 차단 판단 SQL

```sql
SELECT CONTROL_TYPE, BLOCK_YN
  FROM TCF_TRANSACTION_CONTROL
 WHERE SERVICE_ID       = ?
   AND TRANSACTION_CODE = ?
   AND BUSINESS_CODE    = ?
   AND SERVICE_NAME     = ?
   AND USER_ID          = ?
   AND CHANNEL_ID       = ?
   AND BRANCH_ID        = ?
```

## 6. 오류 코드

| 코드 | 의미 |
|------|------|
| `E-TCF-HDR-001` ~ `007` | Header 7필드 누락 |
| `E-TCF-CTL-001` | 거래통제 규칙에 의해 차단 |
| `E-TCF-CTL-002` | 거래통제 데이터 중복 (등록 시) |
| `E-TCF-CTL-003` | 거래통제 Repository 미구성 |

## 7. 구현 파일

| 구분 | 경로 |
|------|------|
| STF 연동 | `tcf-core/.../processor/STF.java` |
| 통제 서비스 | `tcf-core/.../control/TransactionControlService.java` |
| 필수값 검증 | `tcf-core/.../control/TransactionControlValidator.java` |
| Header DTO | `tcf-core/.../control/TransactionControlHeader.java` |
| JDBC Repository | `tcf-web/.../control/JdbcTransactionControlRepository.java` |
| DDL 자동 생성 | `tcf-web/.../control/TransactionControlSchemaInitializer.java` |
| OM 시드 | `tcf-om/.../support/TransactionControlSeedData.java` |

## 8. 운영

- **차단 등록:** OM 거래통제 관리 화면에서 7필드 + 통제유형 + 차단여부(Y) 등록
- **차단 해제:** 동일 Row의 `BLOCK_YN`을 N으로 수정, 또는 Row 삭제
- **설정:** `nsight.tcf.transaction-control-enabled: true` (기본값)

## 9. 최종 원칙

1. Header 7개 항목은 모두 필수
2. 7개 항목이 모두 일치하고 `BLOCK_YN='Y'`이면 차단
3. 미등록 거래는 기본 허용
4. Handler는 통제 통과 요청만 실행
5. `OM.Auth.login/logout/session` 은 거래통제 검사 제외
