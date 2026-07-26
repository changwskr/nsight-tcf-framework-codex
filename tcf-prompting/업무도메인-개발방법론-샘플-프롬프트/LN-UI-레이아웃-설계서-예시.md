# LN(여신) UI 레이아웃 설계서 — 방법론 예시

> 대상 모듈: `ln-service` (업무코드 **LN**) · UI 소스: `tcf-ui/src/main/resources/static/ln/`
> 작성일: 2026-07-26 · 성격: **Phase 5의 입력용 UI 설계서 예시**
> 형식 기준: `ztcf-methodology/EB-UI-레이아웃-설계서.md` (6개 섹션)

---

## 1. 문서 개요

| 항목 | 내용 |
|------|------|
| 문서 목적 | LN 업무 화면의 레이아웃 구조·입력 필드·그리드·이벤트를 정의한다 |
| 대상 서비스 | `ln-service` (bootRun 8103, Tomcat context `/ln`) |
| UI 서빙 모듈 | `tcf-ui` (bootRun 8099) — 정적 HTML/CSS/JS |
| 거래 방식 | tcf-ui 릴레이(`/api/relay/LN/online`) 경유 표준전문(JSON header+body) 호출 |
| 공통 리소스 | `/_shared/online.css`, `/_shared/eb-admin.css`, `/_shared/ui-context.js`, LN 전용 `/_shared/ln-admin.js`(av-admin.js 패턴 복제) |

## 2. 화면 목록

| 화면번호 | 화면명 | URL | 주요 거래(serviceId) | 성격 |
|----------|--------|-----|----------------------|------|
| 21101 | LN 대출 목록 | `/ln/loan-list.html` | `LN.Loan.inquiryList` | 조회 전용(페이징) |

## 3. 공통 레이아웃 구조

EB 공통 레이아웃(`EB-UI-레이아웃-설계서.md` §3)을 그대로 따른다.

| 영역 | 구성 요소 | LN 적용 |
|------|-----------|---------|
| Header | `h1` 제목, 설명문, `nav-links` | "LN 대출 목록" + 업무 목록 이동 링크 |
| Meta Bar | `eb-meta-card` 카드 | 연결 대상 URL / 조회 거래 `LN.Loan.inquiryList`(LN-INQ-0002) |
| Filter | `eb-filter-bar` | 조회 조건 3필드 + 조회/초기화 버튼 |
| Table Panel | `eb-table-panel` + `eb-table` | 대출 목록 그리드 5컬럼 |
| Pagination | `eb-pagination` | pageNo/pageSize(15)/totalCount 기반 |
| Alert/Error | `eb-alert`, alert() | 조회 실패 메시지 |

## 4. 화면별 상세 설계

### 4.1 LN 대출 목록 (`loan-list.html`, 화면 21101)

#### 4.1.1 화면 구성 블록

| 순서 | 블록 | 내용 |
|------|------|------|
| 1 | Header | 제목 "LN 대출 목록", 설명(LN.Loan.inquiryList 페이징 조회 안내), 이동 링크 |
| 2 | Meta Bar | 연결 대상 URL / 조회 거래 `LN.Loan.inquiryList`(LN-INQ-0002) |
| 3 | 조회 필터 바 | 3필드 필터 + 조회/필터 초기화 버튼 |
| 4 | 대출 목록 그리드 | 5컬럼 테이블 + 페이지네이션(페이지당 15건) |

#### 4.1.2 입력 필드 — 조회 필터

| 필드 ID | 라벨 | 타입 | 매칭 방식 | 매핑 body 필드 |
|---------|------|------|-----------|----------------|
| `filterLoanNo` | 대출번호 | text | LIKE 부분 일치 | `loanNo` |
| `filterCustomerId` | 고객번호 | text | = 정확히 일치 | `customerId` |
| `filterLoanStatus` | 대출상태 | select(전체/NORMAL/OVERDUE/CLOSED) | = 정확히 일치 | `loanStatus` |

#### 4.1.3 그리드 컬럼 — 대출 목록

| 순번 | 컬럼명 | 응답 필드 | 표시 형식 |
|------|--------|-----------|-----------|
| 1 | 대출번호 | `loanNo` | 강조(bold) |
| 2 | 고객번호 | `customerId` | 텍스트 |
| 3 | 대출금액 | `loanAmount` | 천단위 콤마 |
| 4 | 대출상태 | `loanStatus` | 상태 칩(NORMAL=ok, OVERDUE=fail, CLOSED=muted) |
| 5 | 등록일시 | `createdAt` | timestamp 포맷 |

#### 4.1.4 이벤트(액션) 정의

| 이벤트 | 트리거 | 처리 |
|--------|--------|------|
| 조회 | `btnSearch` 클릭, 필터 입력 중 Enter | pageNo=1로 `LN.Loan.inquiryList` 호출 → 그리드 갱신 |
| 필터 초기화 | `btnClearFilter` 클릭 | 필터 3필드 비움 → pageNo=1 재조회 |
| 페이지 이동 | 페이지네이션 버튼 | 해당 pageNo로 재조회 |
| 초기 로드 | 페이지 진입 | `loadConfig()` → 연결 대상 표시 → 1페이지 조회 |
| 빈 결과 | rows 0건 | "조회된 대출이 없습니다." 문구 표시 |
| 오류 | 거래 오류·통신 실패 | 그리드에 오류 메시지 행 표시, 페이지네이션 숨김 |

## 5. 화면-거래 매핑 요약

| 화면 | serviceId | transactionCode | processingType | body 필드 |
|------|-----------|-----------------|----------------|-----------|
| `loan-list.html` | `LN.Loan.inquiryList` | LN-INQ-0002 | INQUIRY | pageNo, pageSize(15), loanNo?, customerId?, loanStatus? |

## 6. 공통 UI 규칙

| 규칙 | 내용 |
|------|------|
| 표준전문 header | 전체 필드 필수 (systemId·businessCode·serviceId·transactionCode·processingType·guid·channelId·userId·branchId·requestTime·systemDate·bizDate·clientIp) |
| 페이지 크기 | 15건 고정 (`PAGE_SIZE = 15`) |
| Enter 조회 | 텍스트 필터 필드에서 Enter 시 1페이지 조회 |
| 결과 코드 판정 | `result.resultCode === 'S0000'` 외에는 오류 처리 |
| 릴레이 등록 전제 | `BusinessModuleDefinitions`에 LN(8103) 등록 + `ln-sample-inquiry.json` 존재 |
