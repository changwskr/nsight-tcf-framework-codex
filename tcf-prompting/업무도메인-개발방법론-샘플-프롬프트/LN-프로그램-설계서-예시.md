# LN(여신) 프로그램 설계서 — 방법론 예시

> 대상 모듈: `ln-service` (업무코드 **LN**) · 기준 소스: `com.nh.nsight.marketing.ln`
> 작성일: 2026-07-26 · 성격: **Phase 실행 프롬프트의 입력용 설계서 예시**
> 형식 기준: `ztcf-methodology/EB-프로그램-설계서.md` (9개 섹션)

---

## 1. 시스템 개요

| 항목 | 내용 |
|------|------|
| 모듈명 | `ln-service` — NSIGHT 마케팅 플랫폼 여신 업무 서비스 |
| 업무코드 | `LN` (`nsight.tcf.runtime.business-code=LN`) |
| 메인 클래스 | `com.nh.nsight.marketing.ln.NsightLnServiceApplication` |
| 실행 형태 | Spring Boot WAR (`ln.war`) · bootRun 포트 8103 · Tomcat context `/ln` |
| 거래 엔드포인트 | `POST /online` (bootRun) / `POST /ln/online` (ztomcat) — TCF 파이프라인 단일 진입 |
| DB | H2(local, MODE=Oracle) · MyBatis mapper XML (`classpath:/mapper/ln/*.xml`) |
| 연계 시스템 | 없음 (1차 범위는 조회 전용) |
| 트랜잭션 정책 | 온라인 거래 5초, DB 쿼리 3초 timeout · auto-commit=false |

## 2. 아키텍처(레이어) 설계

EB 표준과 동일한 6계층을 사용한다 (`EB-프로그램-설계서.md` §2 참조).

| 레이어 | 패키지 | LN 구성 |
|--------|--------|---------|
| Handler | `entry.handler` | `LnLoanHandler` 1개 (Loan 도메인) |
| Facade | `entry.facade` | `LnLoanFacade` |
| Service | `application.service` | `LnLoanService` |
| Rule | `application.rule` | `LnLoanRule` |
| DAO/Mapper | `persistence.dao` / `persistence.mapper` + XML | `LnLoanDao` / `LnLoanMapper` |
| DTO | `application.dto.loan`, `persistence.dto.loan` | 요청·응답·Criteria·Row |

## 3. 거래(serviceId) 목록

| serviceId | transactionCode | 유형 | Handler | Facade 메서드 | 설명 |
|-----------|-----------------|------|---------|---------------|------|
| `LN.Loan.inquiry` | LN-INQ-0001 | 조회 | `LnLoanHandler` | `LnLoanFacade.inquiry` | 대출번호 단건 조회 |
| `LN.Loan.inquiryList` | LN-INQ-0002 | 조회 | `LnLoanHandler` | `LnLoanFacade.inquiryList` | 대출 목록 페이징 조회 |

## 4. 프로그램(클래스) 명세

### 4.1 Handler (entry.handler)

| 클래스 | 담당 serviceId | 의존성 | 비고 |
|--------|----------------|--------|------|
| `LnLoanHandler` | `LN.Loan.inquiry`, `LN.Loan.inquiryList` | `LnLoanFacade` | switch 분기, 미지원 시 `SERVICE_NOT_FOUND` |

### 4.2 Facade (entry.facade)

| 클래스 | 메서드 | 트랜잭션 속성 | 입력 → 출력 |
|--------|--------|---------------|-------------|
| `LnLoanFacade` | `inquiry` | readOnly, timeout=5 | body Map → `LoanInquiryRequest` → 응답 Map |
| `LnLoanFacade` | `inquiryList` | readOnly, timeout=5 | body Map → `LoanListInquiryRequest` → 응답 Map |

### 4.3 Service (application.service)

| 클래스 | 메서드 | 처리 로직 |
|--------|--------|-----------|
| `LnLoanService` | `inquiry` | Rule 검증 → `selectLoan` 단건 조회 → 미존재 시 BUSINESS_ERROR("대출번호 미존재") → `LoanInquiryResponse.of` |
| `LnLoanService` | `inquiryList` | Rule 검증 → Criteria 생성 → `searchLoans`+`countLoans` 이중 조회 → `LoanListInquiryResponse.of` |

### 4.4 Rule (application.rule)

| 클래스 | 메서드 | 검증·보정 규칙 |
|--------|--------|----------------|
| `LnLoanRule` | `validateInquiry` | loanNo 필수(StringUtils.hasText), 최대 20자 |
| `LnLoanRule` | `validateInquiryList` | pageSize 최대 100 초과 시 오류, pageNo 1 미만 오류 |
| `LnLoanRule` | `buildSearchCriteria` | 페이징 기본값(pageNo=1, pageSize=15)·offset 계산, customerId·loanStatus trim → null 보정 |

### 4.5 DAO / Mapper (persistence)

| DAO | Mapper (XML) | 주요 메서드 | SQL 요약 |
|-----|--------------|-------------|----------|
| `LnLoanDao` | `LnLoanMapper` (`LnLoanMapper.xml`) | `selectLoan` | `SELECT ... FROM LN_LOAN WHERE LOAN_NO = #{loanNo}` 단건 |
| | | `searchLoans` | 동적 WHERE(customerId =, loanStatus =, loanNo LIKE) · CREATED_AT DESC · OFFSET/FETCH 페이징 |
| | | `countLoans` | 동일 WHERE(`<sql>` 공유) 건수 |

### 4.6 Client / Scheduler / Config

| 클래스 | 구분 | 명세 |
|--------|------|------|
| (없음) | - | 1차 범위는 외부 연계·배치 없음 |

## 5. DTO(전문) 설계

### 5.1 요청 DTO

| DTO | 거래 | 필드 | 비고 |
|-----|------|------|------|
| `LoanInquiryRequest` | LN.Loan.inquiry | loanNo* | `fromMap()` trim 처리 |
| `LoanListInquiryRequest` | LN.Loan.inquiryList | pageNo, pageSize, loanNo, customerId, loanStatus | 빈 문자열 → null |

### 5.2 응답 DTO

| DTO | 거래 | 필드(toMap) | 비고 |
|-----|------|-------------|------|
| `LoanInquiryResponse` | LN.Loan.inquiry | businessCode("LN"), serviceId, guid, loan{loanNo, customerId, loanAmount, loanStatus, createdAt} | 단건 |
| `LoanListInquiryResponse` | LN.Loan.inquiryList | businessCode, serviceId, guid, rows[], totalCount, pageNo, pageSize | 페이징 |

### 5.3 Persistence Row DTO

| Row DTO | 테이블 | 필드 |
|---------|--------|------|
| `LoanRow` | LN_LOAN | loanNo, customerId, loanAmount, loanStatus, createdAt |

## 6. 테이블 설계

| 테이블 | 역할 | 주요 컬럼 |
|--------|------|-----------|
| `LN_LOAN` | 대출 원장 (설계 예시 — local 한정) | LOAN_NO(PK, VARCHAR 20), CUSTOMER_ID(VARCHAR 20, NOT NULL), LOAN_AMOUNT(NUMBER 15, NOT NULL), LOAN_STATUS(VARCHAR 10, 기본 'NORMAL' — NORMAL/OVERDUE/CLOSED), CREATED_AT(TIMESTAMP, 기본 현재시각) |

시드 데이터: 10건 이상 (NORMAL 7·OVERDUE 2·CLOSED 1 — 페이징 2페이지 검증 가능 건수).

## 7. 배치 설계

해당 없음 (1차 범위는 온라인 조회 전용).

## 8. 오류 처리 설계

| 오류 상황 | ErrorCode | 발생 위치 | 메시지 예 |
|-----------|-----------|-----------|-----------|
| 미지원 serviceId | `SERVICE_NOT_FOUND` | `LnLoanHandler` default 분기 | "LnLoanHandler 미지원 serviceId: ..." |
| loanNo 누락 | `BUSINESS_ERROR` | `LnLoanRule.validateInquiry` | "필수 필드 누락: loanNo" |
| 페이징 규칙 위반 | `BUSINESS_ERROR` | `LnLoanRule.validateInquiryList` | "pageSize는 최대 100 입니다." |
| 대출번호 미존재 | `BUSINESS_ERROR` | `LnLoanService.inquiry` | "대출번호 미존재: L999" |

## 9. 환경 설정 요약

| 설정 키 | local 값 | 설명 |
|---------|----------|------|
| `server.port` | 8103 | bootRun 포트 (BusinessModuleDefinitions 예약표와 충돌 없음 확인) |
| `spring.datasource.url` | `jdbc:h2:mem:nsight_ln;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false` | 업무 DB (schema.sql 자동 초기화) |
| `nsight.tcf.transaction-log-datasource.url` | `jdbc:h2:file:.../nsight_om` | 거래로그(OM) 별도 DB — eb-service 값 복사 |
| `nsight.tcf.runtime.business-code` | LN | 업무코드 |
| `nsight.timeout.online-transaction-seconds` / `db-query-seconds` | 5 / 3 | 거래·쿼리 타임아웃 |
